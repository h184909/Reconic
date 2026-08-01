<#
.SYNOPSIS
    Prospektliste for Lan-x: Brønnøysund + DNS/M365-berikelse. Versjon 2.

.CHANGES v2
    - UTF-8 dekoding (fikser Ø/Æ/Å i PS 5.1)
    - Domenefallback: hjemmeside -> epostadresse (henter inn selskaper uten nettside)
    - Ny vinkel: M365 uten sikkerhetslag (Defender/Intune/CA) scorer nå riktig
    - Håndterer null-MX (".") og flere Exchange Online MX-varianter
    - Ny kolonne DomeneKilde og TrengerVerifisering

.EXAMPLE
    .\Get-LanxLeads-v2.ps1 -InkluderUnderenheter -Verbose
#>

[CmdletBinding()]
param(
    [int]    $MinAnsatte  = 25,
    [int]    $MaksAnsatte = 120,
    [switch] $InkluderUnderenheter,
    [string] $OutFile     = ".\lanx-leads-$(Get-Date -Format yyyyMMdd).csv",
    [int]    $ThrottleMs  = 150
)

$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$PSDefaultParameterValues['*:Encoding'] = 'utf8'

# ---------------------------------------------------------------------------
# KONFIGURASJON
# ---------------------------------------------------------------------------

$Kommuner = @{
    '1103' = 'Stavanger'; '1108' = 'Sandnes'; '1124' = 'Sola'; '1127' = 'Randaberg'
    '1120' = 'Klepp';     '1121' = 'Time';    '1122' = 'Gjesdal'; '1119' = 'Hå'
}

$IcpSegmenter = @{
    'Bygg/anlegg'         = @('41','42','43')
    'Industri'            = @('10','16','20','22','23','24','25','28','33')
    'Advokat/regnskap'    = @('69.1','69.2')
    'Radgivning/teknisk'  = @('71.1','71.2')
    'Helse og omsorg'     = @('86','87','88')
    'Transport/logistikk' = @('49','50','52','53')
    'Engros/handel'       = @('45','46')
}

# Sjekkes i rekkefølge - første treff vinner. Utvid etter hvert som du kartlegger.
$MxSignaturer = [ordered]@{
    'mail.protection.outlook.com' = 'Exchange Online (direkte)'
    'mail.eo.outlook.com'         = 'Exchange Online (direkte)'
    'outlook.com'                 = 'Exchange Online (direkte)'
    'google.com'                  = 'Google Workspace'
    'googlemail.com'              = 'Google Workspace'
    'mimecast'                    = 'Mimecast (3.parts filter)'
    'pphosted.com'                = 'Proofpoint (3.parts filter)'
    'ppe-hosted.com'              = 'Proofpoint Essentials (3.parts filter)'
    'barracudanetworks.com'       = 'Barracuda (3.parts filter)'
    'sophos.com'                  = 'Sophos (3.parts filter)'
    'hornetsecurity'              = 'Hornetsecurity/Vade (3.parts filter)'
    'antispamcloud.com'           = 'SpamExperts (typisk mindre MSP/webhotell)'
    'mailanyone.net'              = 'Libraesva/MailAnyone'
    'staysecuregroup.com'         = 'StaySecure (3.parts filter)'
    'altibox.no'                  = 'Altibox (ISP-levert e-post)'
    'domeneshop.no'               = 'Domeneshop (webhotell)'
    'webhuset.no'                 = 'Webhuset (webhotell)'
    'one.com'                     = 'One.com (webhotell)'
    'proisp.no'                   = 'PRO ISP (webhotell)'
}

# ---------------------------------------------------------------------------
# HJELPEFUNKSJONER
# ---------------------------------------------------------------------------

function Invoke-JsonUtf8 {
    <# Invoke-RestMethod i PS 5.1 respekterer ikke charset=utf-8 fra brreg.
       Vi henter rå bytes og dekoder selv. Virker likt i 5.1 og 7. #>
    param([string]$Uri, [int]$TimeoutSec = 30)

    $resp = Invoke-WebRequest -Uri $Uri -TimeoutSec $TimeoutSec `
                              -Headers @{ Accept = 'application/json' } -UseBasicParsing

    $bytes = if ($resp.RawContentStream) { $resp.RawContentStream.ToArray() }
             else                        { [System.Text.Encoding]::Default.GetBytes($resp.Content) }

    return ([System.Text.Encoding]::UTF8.GetString($bytes) | ConvertFrom-Json)
}

function Get-BrregEnheter {
    param([string]$Endpoint, [hashtable]$Query)

    $qs   = ($Query.GetEnumerator() | ForEach-Object { "$($_.Key)=$([uri]::EscapeDataString([string]$_.Value))" }) -join '&'
    $page = 0
    $alle = [System.Collections.Generic.List[object]]::new()

    do {
        $url = "https://data.brreg.no/enhetsregisteret/api/$Endpoint`?$qs&size=1000&page=$page"
        Write-Verbose "GET $url"
        $r = Invoke-JsonUtf8 -Uri $url

        $batch = $r._embedded.$Endpoint
        if ($batch) { $alle.AddRange(@($batch)) }

        $page++
        $totalPages = if ($r.page.totalPages) { $r.page.totalPages } else { 1 }
    } while ($page -lt $totalPages -and $page -lt 50)

    return $alle
}

function Get-IcpSegment {
    param([string]$NaceKode)
    if (-not $NaceKode) { return $null }
    foreach ($seg in $IcpSegmenter.GetEnumerator()) {
        foreach ($p in $seg.Value) { if ($NaceKode.StartsWith($p)) { return $seg.Key } }
    }
    return $null
}

function Get-DomainFromUrl {
    param([string]$Url)
    if ([string]::IsNullOrWhiteSpace($Url)) { return $null }
    $d = ($Url.Trim().ToLower() -replace '^https?://','' -replace '^www\.','')
    $d = ($d -split '[/?#\s]')[0]
    if ($d -match '^[a-z0-9\-\.æøå]+\.[a-z]{2,}$') { return $d }
    return $null
}

function Get-DomainFromEmail {
    param([string]$Email)
    if ([string]::IsNullOrWhiteSpace($Email)) { return $null }
    $e = $Email.Trim().ToLower()
    if ($e -notmatch '@([a-z0-9\-\.æøå]+\.[a-z]{2,})$') { return $null }
    $d = $Matches[1]
    # Frikoble gratis-/regnskapsførerdomener - de sier ingenting om kundens IT
    $ignorer = @('gmail.com','hotmail.com','outlook.com','live.no','online.no',
                 'yahoo.com','icloud.com','me.com','msn.com','broadpark.no','lyse.net')
    if ($ignorer -contains $d) { return $null }
    return $d
}

function Resolve-Safe {
    param([string]$Name, [string]$Type)
    try   { Resolve-DnsName -Name $Name -Type $Type -ErrorAction Stop -DnsOnly }
    catch { $null }
}

function Get-MailInfrastructure {
    param([string]$Domain)

    $out = [ordered]@{
        Mx = ''; MxLeverandor = ''; SpfIncludes = ''; HarSpf = $false
        DmarcPolicy = ''; Autodiscover = ''
    }

    $mx = Resolve-Safe -Name $Domain -Type MX
    if ($mx) {
        $hosts = @($mx | Where-Object { $_.QueryType -eq 'MX' } |
                   Sort-Object Preference | Select-Object -ExpandProperty NameExchange |
                   Where-Object { $_ })
        $out.Mx = ($hosts -join '; ')

        if ($hosts.Count -eq 0 -or ($hosts.Count -eq 1 -and $hosts[0] -in @('.',''))) {
            $out.MxLeverandor = 'Null MX (mottar ikke e-post på dette domenet)'
        } else {
            foreach ($sig in $MxSignaturer.GetEnumerator()) {
                if ($out.Mx -match [regex]::Escape($sig.Key)) { $out.MxLeverandor = $sig.Value; break }
            }
            if (-not $out.MxLeverandor) {
                $labels = @($hosts[0].TrimEnd('.') -split '\.')
                $rot = if ($labels.Count -ge 2) { ($labels[-2..-1]) -join '.' } else { $hosts[0] }
                $out.MxLeverandor = "UKJENT -> $rot"
            }
        }
    }

    $txt = Resolve-Safe -Name $Domain -Type TXT
    if ($txt) {
        $spf = ($txt.Strings | Where-Object { $_ -like 'v=spf1*' }) -join ' '
        if ($spf) {
            $out.HarSpf = $true
            $out.SpfIncludes = (([regex]::Matches($spf,'include:([^\s]+)') |
                                 ForEach-Object { $_.Groups[1].Value }) -join '; ')
        }
    }

    $dmarc = Resolve-Safe -Name "_dmarc.$Domain" -Type TXT
    if ($dmarc) {
        $rec = ($dmarc.Strings | Where-Object { $_ -like 'v=DMARC1*' }) -join ' '
        if ($rec -match 'p=(\w+)') { $out.DmarcPolicy = $Matches[1] }
    }

    $ad = Resolve-Safe -Name "autodiscover.$Domain" -Type CNAME
    if ($ad) {
        $out.Autodiscover = (@($ad | Where-Object { $_.QueryType -eq 'CNAME' } |
                               Select-Object -ExpandProperty NameHost) -join '; ')
    }

    return $out
}

function Get-M365Status {
    param([string]$Domain)
    $out = [ordered]@{ TenantType = 'Ingen M365'; TenantId = ''; Federert = $false }

    try {
        $realm = Invoke-JsonUtf8 -TimeoutSec 10 -Uri `
            "https://login.microsoftonline.com/getuserrealm.srf?login=user@$Domain&json=1"
        switch ($realm.NameSpaceType) {
            'Managed'   { $out.TenantType = 'Entra ID (managed)' }
            'Federated' {
                $idp = ($realm.AuthURL -replace 'https?://([^/]+).*','$1')
                $out.TenantType = "Federert ($idp)"
                $out.Federert   = $true
            }
        }
    } catch { }

    if ($out.TenantType -ne 'Ingen M365') {
        try {
            $oidc = Invoke-JsonUtf8 -TimeoutSec 10 -Uri `
                "https://login.microsoftonline.com/$Domain/v2.0/.well-known/openid-configuration"
            if ($oidc.issuer -match '/([0-9a-f\-]{36})/') { $out.TenantId = $Matches[1] }
        } catch { }
    }

    return $out
}

function Get-LeadScore {
    param($Row)

    $score = 0
    $v = [System.Collections.Generic.List[string]]::new()

    $harM365 = $Row.TenantType -like 'Entra*' -or $Row.Federert
    $harFilter = $Row.MxLeverandor -match '3\.parts filter'

    # --- Migrering bort fra on-prem ---
    if ($Row.TenantType -eq 'Ingen M365' -and $Row.Domene) {
        $score += 30; $v.Add('MIGRERING: ingen M365-tenant funnet')
    }
    if ($Row.Federert) {
        $score += 25; $v.Add('MIGRERING: federert (AD FS) = on-prem avhengighet')
    }
    if ($Row.Autodiscover -and $Row.Autodiscover -notmatch 'outlook\.com') {
        $score += 10; $v.Add('MIGRERING: autodiscover peker vekk fra Exchange Online')
    }

    # --- M365 uten sikkerhetslag: Lan-x kjernepitch ---
    if ($harM365 -and -not $harFilter) {
        $score += 25
        $v.Add('SIKKERHET: M365 uten 3.parts filter - Defender for Office/Intune-baseline/CA er usolgt')
    }

    # --- Sikkerhet / compliance ---
    if ($Row.Domene) {
        if (-not $Row.HarSpf)                { $score += 20; $v.Add('SIKKERHET: mangler SPF') }
        if (-not $Row.DmarcPolicy)           { $score += 20; $v.Add('SIKKERHET: mangler DMARC') }
        elseif ($Row.DmarcPolicy -eq 'none') { $score += 10; $v.Add('SIKKERHET: DMARC p=none, ikke håndhevet') }
    }
    if ($Row.Segment -in @('Advokat/regnskap','Helse og omsorg')) {
        $score += 15; $v.Add('COMPLIANCE: regulert bransje, NIS2/GDPR-driver')
    }

    # --- Pris mot dagens leverandør ---
    if ($Row.MxLeverandor -like 'UKJENT*') {
        $score += 15; $v.Add('PRIS: identifiserbar MSP i MX - kan prises mot')
    }
    if ($harFilter) {
        $score += 10; $v.Add('PRIS: betaler for 3.parts filter, Defender kan konsolidere')
    }

    # --- Nærhet ---
    if ($Row.MxLeverandor -match 'webhotell|ISP-levert') {
        $score += 15; $v.Add('NÆRHET: e-post hos webhotell/ISP, ingen reell driftspartner')
    }

    # --- Størrelse ---
    $avvik = [Math]::Abs([int]$Row.AntallAnsatte - 50)
    if ($avvik -le 15) { $score += 15 } elseif ($avvik -le 30) { $score += 8 }

    # --- Straff for manglende datagrunnlag ---
    if (-not $Row.Domene) {
        $score = [Math]::Round($score * 0.5)
        $v.Add('OBS: ingen domene funnet - alt over er uverifisert, ring for å kartlegge')
    }

    [pscustomobject]@{ Score = $score; Vinkler = ($v -join ' | ') }
}

# ---------------------------------------------------------------------------
# 1. HENT
# ---------------------------------------------------------------------------

Write-Host "[1/3] Henter fra Enhetsregisteret..." -ForegroundColor Cyan
$kommuneListe = ($Kommuner.Keys) -join ','

function ConvertTo-Kandidat {
    param($E, [string]$Type, [string]$AdrFelt)
    $seg = Get-IcpSegment -NaceKode $E.naeringskode1.kode
    if (-not $seg) { return }

    $adr = $E.$AdrFelt
    $dom = Get-DomainFromUrl $E.hjemmeside
    $kilde = 'hjemmeside'
    if (-not $dom) { $dom = Get-DomainFromEmail $E.epostadresse; $kilde = 'epost' }
    if (-not $dom) { $kilde = 'ingen' }

    [pscustomobject]@{
        Navn = $E.navn; OrgNr = $E.organisasjonsnummer
        AntallAnsatte = [int]$E.antallAnsatte; Segment = $seg
        Nace = "$($E.naeringskode1.kode) $($E.naeringskode1.beskrivelse)"
        Kommune = $adr.kommune; Adresse = ($adr.adresse -join ', ')
        Telefon = $E.telefon; Epost = $E.epostadresse; Hjemmeside = $E.hjemmeside
        Domene = $dom; DomeneKilde = $kilde; Type = $Type
    }
}

$kandidater = [System.Collections.Generic.List[object]]::new()

Get-BrregEnheter -Endpoint 'enheter' -Query @{
    organisasjonsform = 'AS'; kommunenummer = $kommuneListe
    fraAntallAnsatte  = $MinAnsatte; tilAntallAnsatte = $MaksAnsatte
} | Where-Object { -not $_.konkurs -and -not $_.underAvvikling } | ForEach-Object {
    $k = ConvertTo-Kandidat -E $_ -Type 'Hovedenhet' -AdrFelt 'forretningsadresse'
    if ($k) { $kandidater.Add($k) }
}

if ($InkluderUnderenheter) {
    Get-BrregEnheter -Endpoint 'underenheter' -Query @{
        kommunenummer = $kommuneListe
        fraAntallAnsatte = $MinAnsatte; tilAntallAnsatte = $MaksAnsatte
    } | ForEach-Object {
        $k = ConvertTo-Kandidat -E $_ -Type "Underenhet av $($_.overordnetEnhet)" -AdrFelt 'beliggenhetsadresse'
        if ($k) { $kandidater.Add($k) }
    }
}

$medDomene = ($kandidater | Where-Object Domene).Count
Write-Host ("      {0} kandidater, {1} med domene ({2:P0} dekning)" -f `
    $kandidater.Count, $medDomene, ($medDomene / [Math]::Max($kandidater.Count,1))) -ForegroundColor Green

# ---------------------------------------------------------------------------
# 2. BERIK
# ---------------------------------------------------------------------------

Write-Host "[2/3] Beriker med MX/SPF/DMARC og M365-status..." -ForegroundColor Cyan

$i = 0
$beriket = foreach ($k in $kandidater) {
    $i++
    Write-Progress -Activity 'Berikelse' -Status "$i / $($kandidater.Count)  $($k.Navn)" `
                   -PercentComplete (100 * $i / [Math]::Max($kandidater.Count,1))

    $mail = if ($k.Domene) { Get-MailInfrastructure -Domain $k.Domene } else { @{} }
    $m365 = if ($k.Domene) { Get-M365Status -Domain $k.Domene } else { @{ TenantType = 'Ikke sjekket' } }

    $row = [pscustomobject]@{
        Navn = $k.Navn; OrgNr = $k.OrgNr; AntallAnsatte = $k.AntallAnsatte
        Segment = $k.Segment; Kommune = $k.Kommune; Type = $k.Type
        Domene = $k.Domene; DomeneKilde = $k.DomeneKilde
        Telefon = $k.Telefon; Epost = $k.Epost
        MxLeverandor = $mail.MxLeverandor; Mx = $mail.Mx
        HarSpf = [bool]$mail.HarSpf; SpfIncludes = $mail.SpfIncludes
        DmarcPolicy = $mail.DmarcPolicy; Autodiscover = $mail.Autodiscover
        TenantType = $m365.TenantType; Federert = [bool]$m365.Federert; TenantId = $m365.TenantId
        Nace = $k.Nace; Adresse = $k.Adresse
    }

    $s = Get-LeadScore -Row $row
    $row | Add-Member -NotePropertyName Score              -NotePropertyValue $s.Score -PassThru |
           Add-Member -NotePropertyName Vinkler            -NotePropertyValue $s.Vinkler -PassThru |
           Add-Member -NotePropertyName TrengerVerifisering -NotePropertyValue (-not $k.Domene) -PassThru

    if ($k.Domene) { Start-Sleep -Milliseconds $ThrottleMs }
}

# ---------------------------------------------------------------------------
# 3. EKSPORT
# ---------------------------------------------------------------------------

$sortert = $beriket | Sort-Object Score -Descending
$sortert | Export-Csv -Path $OutFile -Encoding UTF8 -NoTypeInformation -Delimiter ';'
Write-Host "`nFerdig. $($sortert.Count) leads -> $OutFile`n" -ForegroundColor Green

Write-Host "Topp 20 med verifisert datagrunnlag:" -ForegroundColor Yellow
$sortert | Where-Object { -not $_.TrengerVerifisering } | Select-Object -First 20 `
    Score, Navn, AntallAnsatte, Segment, TenantType, MxLeverandor, DmarcPolicy |
    Format-Table -AutoSize

Write-Host "`nLeverandørfordeling:" -ForegroundColor Yellow
$sortert | Where-Object MxLeverandor | Group-Object MxLeverandor |
    Sort-Object Count -Descending | Select-Object Count, Name | Format-Table -AutoSize

Write-Host "Ukjente MX-roter - legg de hyppigste inn i `$MxSignaturer:" -ForegroundColor Yellow
$sortert | Where-Object { $_.MxLeverandor -like 'UKJENT*' } |
    Group-Object MxLeverandor | Sort-Object Count -Descending |
    Select-Object -First 20 Count, Name | Format-Table -AutoSize
