(function() {
    function setupStickyTableEnhancements(options) {
        if (!options || !options.tableRootSelector) {
            return;
        }

        var tableRoot = document.querySelector(options.tableRootSelector);
        if (!tableRoot) {
            return;
        }

        var stickyBarId = options.stickyBarId || (tableRoot.id + '-sticky-scroll');
        var stickyBar = document.getElementById(stickyBarId);
        if (!stickyBar) {
            stickyBar = document.createElement('div');
            stickyBar.id = stickyBarId;
            stickyBar.className = 'fp-sticky-horizontal-scroll';
            stickyBar.innerHTML = '<div class="fp-sticky-horizontal-scroll-inner"></div>';
            tableRoot.parentNode.insertBefore(stickyBar, tableRoot);
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

        function updateStickyBar() {
            var holder = getHolderElement();
            var table = getTableElement();
            if (!holder || !table) {
                stickyBar.style.display = 'none';
                return;
            }

            var needsHorizontalScroll = table.scrollWidth > holder.clientWidth + 1;
            stickyBar.style.display = needsHorizontalScroll ? 'block' : 'none';
            stickyBar.querySelector('.fp-sticky-horizontal-scroll-inner').style.width = table.scrollWidth + 'px';
            stickyBar.scrollLeft = holder.scrollLeft;
        }

        function syncFromStickyBar() {
            var holder = getHolderElement();
            if (!holder) {
                return;
            }
            holder.scrollLeft = stickyBar.scrollLeft;
        }

        function syncFromHolder() {
            stickyBar.scrollLeft = this.scrollLeft;
        }

        function applyStickyHeaderOffset() {
            var header = getHeaderElement();
            if (!header) {
                return;
            }
            header.style.position = 'sticky';
            header.style.top = (options.headerTop || 96) + 'px';
            header.style.zIndex = '30';
            header.style.background = '#fff';
            header.style.boxShadow = '0 2px 8px rgba(0,0,0,0.05)';
        }

        function refresh() {
            applyStickyHeaderOffset();
            updateStickyBar();
            var holder = getHolderElement();
            if (holder && !holder.dataset.fpStickyScrollBound) {
                holder.addEventListener('scroll', syncFromHolder, { passive: true });
                holder.dataset.fpStickyScrollBound = 'true';
            }
        }

        stickyBar.addEventListener('scroll', syncFromStickyBar, { passive: true });
        window.addEventListener('resize', refresh);
        refresh();

        return {
            refresh: refresh
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
