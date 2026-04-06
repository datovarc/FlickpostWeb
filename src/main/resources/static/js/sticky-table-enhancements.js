(function() {
    function setupStickyTableEnhancements(options) {
        if (!options || !options.tableRootSelector) {
            return null;
        }

        var tableRoot = document.querySelector(options.tableRootSelector);
        if (!tableRoot) {
            return null;
        }

        var shell = tableRoot.closest('.fp-table-shell') || tableRoot.parentNode;
        var stickyHeaderId = options.stickyHeaderId || (tableRoot.id + '-sticky-header');
        var stickyBarId = options.stickyBarId || (tableRoot.id + '-sticky-scroll');
        var stickyTop = options.stickyTop || 124;

        var stickyHeader = document.getElementById(stickyHeaderId);
        if (!stickyHeader) {
            stickyHeader = document.createElement('div');
            stickyHeader.id = stickyHeaderId;
            stickyHeader.className = 'fp-sticky-table-header';
            stickyHeader.style.display = 'none';
            document.body.appendChild(stickyHeader);
        }

        var stickyBar = document.getElementById(stickyBarId);
        if (!stickyBar) {
            stickyBar = document.createElement('div');
            stickyBar.id = stickyBarId;
            stickyBar.className = 'fp-sticky-horizontal-scroll fp-sticky-horizontal-scroll-floating';
            stickyBar.style.display = 'none';
            stickyBar.innerHTML = '<div class="fp-sticky-horizontal-scroll-inner"></div>';
            document.body.appendChild(stickyBar);
        }

        function getHeaderElement() {
            return tableRoot.querySelector('.tabulator-header');
        }

        function getHolderElement() {
            return tableRoot.querySelector('.tabulator-tableholder, .tabulator-tableHolder');
        }

        function getTableElement() {
            return tableRoot.querySelector('.tabulator-table');
        }

        function syncHeaderScroll(left) {
            var stickyHeaderHolder = stickyHeader.querySelector('.tabulator-header');
            if (stickyHeaderHolder) {
                stickyHeaderHolder.scrollLeft = left;
            }
        }

        function syncFromStickyBar() {
            var holder = getHolderElement();
            if (!holder) {
                return;
            }
            holder.scrollLeft = stickyBar.scrollLeft;
            syncHeaderScroll(stickyBar.scrollLeft);
        }

        function syncFromHolder() {
            stickyBar.scrollLeft = this.scrollLeft;
            syncHeaderScroll(this.scrollLeft);
        }

        function updateStickyElements() {
            var header = getHeaderElement();
            var holder = getHolderElement();
            var table = getTableElement();
            if (!header || !holder || !table) {
                stickyHeader.style.display = 'none';
                stickyBar.style.display = 'none';
                return;
            }

            var shellRect = shell.getBoundingClientRect();
            var tableRect = tableRoot.getBoundingClientRect();
            var needsHorizontalScroll = table.scrollWidth > holder.clientWidth + 1;
            var viewportActive = shellRect.top < stickyTop && shellRect.bottom > stickyTop + 80;

            if (viewportActive) {
                stickyHeader.style.display = 'block';
                stickyHeader.style.position = 'fixed';
                stickyHeader.style.top = stickyTop + 'px';
                stickyHeader.style.left = shellRect.left + 'px';
                stickyHeader.style.width = shellRect.width + 'px';
                stickyHeader.style.zIndex = '60';
                stickyHeader.innerHTML = '';
                stickyHeader.appendChild(header.cloneNode(true));
                syncHeaderScroll(holder.scrollLeft);
            } else {
                stickyHeader.style.display = 'none';
            }

            if (viewportActive && needsHorizontalScroll) {
                stickyBar.style.display = 'block';
                stickyBar.style.position = 'fixed';
                stickyBar.style.top = (stickyTop + 48) + 'px';
                stickyBar.style.left = shellRect.left + 'px';
                stickyBar.style.width = shellRect.width + 'px';
                stickyBar.style.zIndex = '55';
                stickyBar.querySelector('.fp-sticky-horizontal-scroll-inner').style.width = table.scrollWidth + 'px';
                stickyBar.scrollLeft = holder.scrollLeft;
            } else {
                stickyBar.style.display = 'none';
            }
        }

        function refresh() {
            var holder = getHolderElement();
            if (holder && !holder.dataset.fpStickyScrollBound) {
                holder.addEventListener('scroll', syncFromHolder, { passive: true });
                holder.dataset.fpStickyScrollBound = 'true';
            }
            updateStickyElements();
        }

        stickyBar.addEventListener('scroll', syncFromStickyBar, { passive: true });
        window.addEventListener('resize', refresh);
        window.addEventListener('scroll', updateStickyElements, { passive: true });
        refresh();

        return {
            refresh: refresh,
            updateStickyElements: updateStickyElements
        };
    }

    function setupStickyScanSessionSummary(summarySelector) {
        var summary = document.querySelector(summarySelector);
        if (!summary) {
            return;
        }

        function updateSummaryState() {
            if (window.scrollY > 120) {
                summary.classList.add('scan-session-latest-summary-condensed');
            } else {
                summary.classList.remove('scan-session-latest-summary-condensed');
            }
        }

        window.addEventListener('scroll', updateSummaryState, { passive: true });
        updateSummaryState();
    }

    window.setupStickyTableEnhancements = setupStickyTableEnhancements;
    window.setupStickyScanSessionSummary = setupStickyScanSessionSummary;
})();
