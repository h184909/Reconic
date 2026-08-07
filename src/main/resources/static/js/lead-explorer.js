(() => {
    "use strict";

    const root = document.getElementById("leadExplorer");
    if (!root) {
        return;
    }

    const body = document.getElementById("leadExplorerBody");
    if (!body) {
        return;
    }

    const rows = Array.from(body.querySelectorAll(".lead-row"));
    const totalCount = rows.length;

    const els = {
        text: document.getElementById("filterText"),
        employeesMin: document.getElementById("filterEmployeesMin"),
        employeesMax: document.getElementById("filterEmployeesMax"),
        municipality: document.getElementById("filterMunicipality"),
        segment: document.getElementById("filterSegment"),
        platform: document.getElementById("filterPlatform"),
        provider: document.getElementById("filterProvider"),
        providerRole: document.getElementById("filterProviderRole"),
        dnssec: document.getElementById("filterDnssec"),
        domainConfidence: document.getElementById("filterDomainConfidence"),
        priority: document.getElementById("filterPriority"),
        sort: document.getElementById("filterSort"),
        requireDomain: document.getElementById("filterRequireDomain"),
        requireMx: document.getElementById("filterRequireMx"),
        onlyMsp: document.getElementById("filterOnlyMsp"),
        pageSize: document.getElementById("filterPageSize"),
        visibleCount: document.getElementById("explorerVisibleCount"),
        totalCount: document.getElementById("explorerTotalCount"),
        averageScore: document.getElementById("explorerAverageScore"),
        highPriorityCount: document.getElementById("explorerHighPriorityCount"),
        mspCount: document.getElementById("explorerMspCount"),
        weakMailCount: document.getElementById("explorerWeakMailCount"),
        statusText: document.getElementById("explorerStatusText"),
        activeFilterChips: document.getElementById("activeFilterChips"),
        providerSummaryChips: document.getElementById("providerSummaryChips"),
        emptyFiltered: document.getElementById("explorerEmptyFiltered"),
        pagination: document.getElementById("explorerPagination"),
        pageStatus: document.getElementById("explorerPageStatus"),
        prevPage: document.getElementById("explorerPrevPage"),
        nextPage: document.getElementById("explorerNextPage"),
        clear: document.getElementById("clearExplorerFilters"),
        coldCallPreset: document.getElementById("presetColdCall"),
        exportFiltered: document.getElementById("exportFilteredCsv")
    };

    let currentPage = 1;
    let filteredRows = [...rows];

    const textValue = value => (value ?? "").toString().trim();
    const normalized = value => textValue(value).toLocaleLowerCase("nb-NO");
    const intValue = value => {
        const number = Number.parseInt(textValue(value), 10);
        return Number.isFinite(number) ? number : 0;
    };

    function providerTokens(row) {
        return Array.from(row.querySelectorAll(".provider-filter-token")).map(token => ({
            provider: textValue(token.dataset.provider),
            role: textValue(token.dataset.providerRole)
        }));
    }

    function uniqueSorted(values) {
        return [...new Set(values.filter(Boolean))]
            .sort((a, b) => a.localeCompare(b, "nb-NO"));
    }

    function addOptions(select, values) {
        if (!select) return;
        uniqueSorted(values).forEach(value => {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value;
            select.append(option);
        });
    }

    function populateDynamicFilters() {
        addOptions(els.municipality, rows.map(row => row.dataset.municipality));
        addOptions(els.segment, uniqueSorted(rows.map(row => row.dataset.segmentLabel)));

        const providers = [];
        rows.forEach(row => providerTokens(row).forEach(token => providers.push(token.provider)));
        addOptions(els.provider, providers);
    }

    function selectedCheckboxValues(group) {
        return Array.from(document.querySelectorAll(`input[data-filter-group="${group}"]:checked`))
            .map(input => input.value);
    }

    function selectedCheckboxLabels(group) {
        return Array.from(document.querySelectorAll(`input[data-filter-group="${group}"]:checked`))
            .map(input => input.parentElement?.textContent?.trim() || input.value);
    }

    function rowSearchText(row) {
        return normalized([
            row.dataset.name,
            row.dataset.orgnr,
            row.dataset.domain,
            row.dataset.email,
            row.dataset.phone,
            row.dataset.address,
            row.dataset.municipality,
            row.dataset.segmentLabel,
            ...providerTokens(row).map(token => token.provider)
        ].join(" "));
    }

    function matchesRow(row) {
        const query = normalized(els.text?.value);
        if (query && !rowSearchText(row).includes(query)) {
            return false;
        }

        const employees = intValue(row.dataset.employees);
        const minEmployees = textValue(els.employeesMin?.value);
        const maxEmployees = textValue(els.employeesMax?.value);
        if (minEmployees && employees < intValue(minEmployees)) return false;
        if (maxEmployees && employees > intValue(maxEmployees)) return false;

        if (els.municipality?.value && row.dataset.municipality !== els.municipality.value) return false;
        if (els.segment?.value && row.dataset.segmentLabel !== els.segment.value) return false;
        if (els.platform?.value && row.dataset.platform !== els.platform.value) return false;
        if (els.dnssec?.value && row.dataset.dnssec !== els.dnssec.value) return false;
        if (els.domainConfidence?.value && row.dataset.domainConfidence !== els.domainConfidence.value) return false;
        if (els.priority?.value && row.dataset.priority !== els.priority.value) return false;

        if (els.requireDomain?.checked && row.dataset.hasDomain !== "true") return false;
        if (els.requireMx?.checked && row.dataset.hasMx !== "true") return false;

        const dmarcValues = selectedCheckboxValues("dmarc");
        if (dmarcValues.length && !dmarcValues.includes(row.dataset.dmarc)) return false;

        const spfValues = selectedCheckboxValues("spf");
        if (spfValues.length && !spfValues.includes(row.dataset.spf)) return false;

        const tokens = providerTokens(row);
        if (els.provider?.value) {
            if (els.provider.value === "__NONE__") {
                if (tokens.length > 0) return false;
            } else if (!tokens.some(token => token.provider === els.provider.value)) {
                return false;
            }
        }

        if (els.providerRole?.value && !tokens.some(token => token.role === els.providerRole.value)) {
            return false;
        }

        if (els.onlyMsp?.checked && !tokens.some(token => token.role === "MSP_CANDIDATE")) {
            return false;
        }

        return true;
    }

    function compareRows(a, b) {
        const sort = els.sort?.value || "opportunity-desc";
        if (sort === "employees-desc") {
            return intValue(b.dataset.employees) - intValue(a.dataset.employees)
                || normalized(a.dataset.name).localeCompare(normalized(b.dataset.name), "nb-NO");
        }
        if (sort === "confidence-desc") {
            return intValue(b.dataset.dataConfidence) - intValue(a.dataset.dataConfidence)
                || intValue(b.dataset.score) - intValue(a.dataset.score);
        }
        if (sort === "company-asc") {
            return textValue(a.dataset.name).localeCompare(textValue(b.dataset.name), "nb-NO");
        }
        return intValue(b.dataset.score) - intValue(a.dataset.score)
            || intValue(b.dataset.dataConfidence) - intValue(a.dataset.dataConfidence)
            || intValue(b.dataset.employees) - intValue(a.dataset.employees);
    }

    function pageSize() {
        if (els.pageSize?.value === "all") {
            return Math.max(filteredRows.length, 1);
        }
        const parsed = intValue(els.pageSize?.value);
        return parsed > 0 ? parsed : 50;
    }

    function totalPages() {
        return Math.max(1, Math.ceil(filteredRows.length / pageSize()));
    }

    function activeFilterLabels() {
        const labels = [];
        if (textValue(els.text?.value)) labels.push(`Søk: ${els.text.value.trim()}`);
        if (textValue(els.employeesMin?.value)) labels.push(`Ansatte ≥ ${els.employeesMin.value}`);
        if (textValue(els.employeesMax?.value)) labels.push(`Ansatte ≤ ${els.employeesMax.value}`);
        if (els.municipality?.value) labels.push(els.municipality.value);
        if (els.segment?.value) labels.push(els.segment.value);
        if (els.platform?.value) labels.push(`Plattform: ${selectedText(els.platform)}`);
        if (els.provider?.value) labels.push(`Leverandør: ${selectedText(els.provider)}`);
        if (els.providerRole?.value) labels.push(`Rolle: ${selectedText(els.providerRole)}`);
        if (els.dnssec?.value) labels.push(`DNSSEC: ${selectedText(els.dnssec)}`);
        if (els.domainConfidence?.value) labels.push(`Domenetillit: ${selectedText(els.domainConfidence)}`);
        if (els.priority?.value) labels.push(`Prioritet: ${selectedText(els.priority)}`);
        labels.push(...selectedCheckboxLabels("dmarc").map(value => `DMARC: ${value}`));
        labels.push(...selectedCheckboxLabels("spf").map(value => `SPF: ${value}`));
        if (els.requireDomain?.checked) labels.push("Krever domene");
        if (els.requireMx?.checked) labels.push("Krever MX");
        if (els.onlyMsp?.checked) labels.push("Kun mulig MSP");
        return labels;
    }

    function selectedText(select) {
        return select?.selectedOptions?.[0]?.textContent?.trim() || select?.value || "";
    }

    function renderActiveChips() {
        if (!els.activeFilterChips) return;
        els.activeFilterChips.replaceChildren();
        activeFilterLabels().forEach(label => {
            const chip = document.createElement("span");
            chip.className = "filter-chip";
            chip.textContent = label;
            els.activeFilterChips.append(chip);
        });
    }

    function renderProviderSummary() {
        if (!els.providerSummaryChips) return;

        const counts = new Map();
        filteredRows.forEach(row => {
            providerTokens(row).forEach(token => {
                counts.set(token.provider, (counts.get(token.provider) || 0) + 1);
            });
        });

        els.providerSummaryChips.replaceChildren();
        [...counts.entries()]
            .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], "nb-NO"))
            .slice(0, 8)
            .forEach(([provider, count]) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "provider-summary-chip";
                button.innerHTML = `<span>${escapeHtml(provider)}</span><strong>${count}</strong>`;
                button.addEventListener("click", () => {
                    if (els.provider) {
                        els.provider.value = provider;
                        currentPage = 1;
                        applyFilters();
                    }
                });
                els.providerSummaryChips.append(button);
            });

        if (counts.size === 0) {
            const empty = document.createElement("span");
            empty.className = "muted";
            empty.textContent = "Ingen kjente leverandørsignaler i utvalget.";
            els.providerSummaryChips.append(empty);
        }
    }

    function renderMetrics() {
        if (els.visibleCount) els.visibleCount.textContent = filteredRows.length.toString();
        if (els.totalCount) els.totalCount.textContent = totalCount.toString();

        const scores = filteredRows.map(row => intValue(row.dataset.score));
        const average = scores.length
            ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
            : 0;
        if (els.averageScore) els.averageScore.textContent = average.toString();

        const high = filteredRows.filter(row =>
            row.dataset.priority === "HIGH" || row.dataset.priority === "VERY_HIGH"
        ).length;
        if (els.highPriorityCount) els.highPriorityCount.textContent = high.toString();

        const msp = filteredRows.filter(row =>
            providerTokens(row).some(token => token.role === "MSP_CANDIDATE")
        ).length;
        if (els.mspCount) els.mspCount.textContent = msp.toString();

        const weakMail = filteredRows.filter(row =>
            ["MISSING", "MONITORING"].includes(row.dataset.dmarc)
            && ["MISSING", "SOFT_FAIL", "MULTIPLE"].includes(row.dataset.spf)
        ).length;
        if (els.weakMailCount) els.weakMailCount.textContent = weakMail.toString();

        if (els.statusText) {
            const activeCount = activeFilterLabels().length;
            els.statusText.textContent = activeCount
                ? `Viser ${filteredRows.length} av ${totalCount} kandidater`
                : `Viser alle ${totalCount} kandidater`;
        }
    }

    function renderPage() {
        const size = pageSize();
        const pages = totalPages();
        currentPage = Math.min(Math.max(currentPage, 1), pages);

        const start = (currentPage - 1) * size;
        const end = start + size;
        const pageRows = new Set(filteredRows.slice(start, end));

        rows.forEach(row => {
            row.hidden = !pageRows.has(row);
        });

        if (els.emptyFiltered) {
            els.emptyFiltered.hidden = filteredRows.length !== 0;
        }

        if (els.pagination) {
            els.pagination.hidden = filteredRows.length === 0 || els.pageSize?.value === "all";
        }
        if (els.pageStatus) {
            els.pageStatus.textContent = `Side ${currentPage} av ${pages}`;
        }
        if (els.prevPage) els.prevPage.disabled = currentPage <= 1;
        if (els.nextPage) els.nextPage.disabled = currentPage >= pages;
    }

    function applyFilters() {
        filteredRows = rows.filter(matchesRow).sort(compareRows);

        // Reorder DOM so selected sorting also applies when pagination is changed later.
        filteredRows.forEach(row => body.append(row));
        rows.filter(row => !filteredRows.includes(row)).forEach(row => body.append(row));

        renderMetrics();
        renderActiveChips();
        renderProviderSummary();
        renderPage();
    }

    function clearFilters() {
        [
            els.text, els.employeesMin, els.employeesMax, els.municipality,
            els.segment, els.platform, els.provider, els.providerRole,
            els.dnssec, els.domainConfidence, els.priority
        ].forEach(element => {
            if (element) element.value = "";
        });

        document.querySelectorAll("input[data-filter-group]").forEach(input => {
            input.checked = false;
        });

        [els.requireDomain, els.requireMx, els.onlyMsp].forEach(element => {
            if (element) element.checked = false;
        });

        if (els.sort) els.sort.value = "opportunity-desc";
        currentPage = 1;
        applyFilters();
    }

    function applyColdCallPreset() {
        clearFilters();

        if (els.requireDomain) els.requireDomain.checked = true;
        if (els.requireMx) els.requireMx.checked = true;
        if (els.domainConfidence) els.domainConfidence.value = "HIGH";

        setChecked("dmarc", ["MISSING", "MONITORING"]);
        setChecked("spf", ["MISSING", "SOFT_FAIL", "MULTIPLE"]);

        currentPage = 1;
        applyFilters();
    }

    function setChecked(group, values) {
        document.querySelectorAll(`input[data-filter-group="${group}"]`).forEach(input => {
            input.checked = values.includes(input.value);
        });
    }

    function csvEscape(value) {
        const safe = textValue(value);
        if (safe.includes(";") || safe.includes("\"") || safe.includes("\n") || safe.includes("\r")) {
            return `"${safe.replaceAll("\"", "\"\"")}"`;
        }
        return safe;
    }

    function exportFilteredCsv() {
        const header = [
            "organizationNumber",
            "companyName",
            "employees",
            "segment",
            "municipality",
            "domain",
            "registeredEmail",
            "phone",
            "emailPlatform",
            "dmarc",
            "spf",
            "dnssec",
            "providerSignals",
            "opportunityScore",
            "priority",
            "dataConfidence"
        ];

        const lines = [header.join(";")];
        filteredRows.forEach(row => {
            const providers = providerTokens(row).map(token => `${token.provider} [${token.role}]`).join(" | ");
            lines.push([
                row.dataset.orgnr,
                row.dataset.name,
                row.dataset.employees,
                row.dataset.segmentLabel,
                row.dataset.municipality,
                row.dataset.domain,
                row.dataset.email,
                row.dataset.phone,
                row.dataset.platform,
                row.dataset.dmarc,
                row.dataset.spf,
                row.dataset.dnssec,
                providers,
                row.dataset.score,
                row.dataset.priority,
                row.dataset.dataConfidence
            ].map(csvEscape).join(";"));
        });

        const blob = new Blob(["\uFEFF" + lines.join("\r\n")], {
            type: "text/csv;charset=utf-8"
        });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = `reconic-filtered-${timestampForFilename()}.csv`;
        document.body.append(anchor);
        anchor.click();
        anchor.remove();
        URL.revokeObjectURL(url);
    }

    function timestampForFilename() {
        const date = new Date();
        const pad = value => value.toString().padStart(2, "0");
        return [
            date.getFullYear(),
            pad(date.getMonth() + 1),
            pad(date.getDate()),
            "-",
            pad(date.getHours()),
            pad(date.getMinutes()),
            pad(date.getSeconds())
        ].join("");
    }

    function escapeHtml(value) {
        return textValue(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#039;");
    }

    function bindEvents() {
        const instantControls = [
            els.text, els.employeesMin, els.employeesMax, els.municipality,
            els.segment, els.platform, els.provider, els.providerRole,
            els.dnssec, els.domainConfidence, els.priority, els.sort,
            els.requireDomain, els.requireMx, els.onlyMsp
        ].filter(Boolean);

        instantControls.forEach(element => {
            const eventName = element.matches("input[type='search'], input[type='number']") ? "input" : "change";
            element.addEventListener(eventName, () => {
                currentPage = 1;
                applyFilters();
            });
        });

        document.querySelectorAll("input[data-filter-group]").forEach(input => {
            input.addEventListener("change", () => {
                currentPage = 1;
                applyFilters();
            });
        });

        els.pageSize?.addEventListener("change", () => {
            currentPage = 1;
            renderPage();
        });

        els.prevPage?.addEventListener("click", () => {
            if (currentPage > 1) {
                currentPage -= 1;
                renderPage();
            }
        });

        els.nextPage?.addEventListener("click", () => {
            if (currentPage < totalPages()) {
                currentPage += 1;
                renderPage();
            }
        });

        els.clear?.addEventListener("click", clearFilters);
        els.coldCallPreset?.addEventListener("click", applyColdCallPreset);
        els.exportFiltered?.addEventListener("click", exportFilteredCsv);
    }

    populateDynamicFilters();
    bindEvents();
    applyFilters();
})();
