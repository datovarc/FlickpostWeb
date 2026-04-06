(function() {
    var requestInFlight = false;

    function getButton() {
        return document.getElementById('recheckPendingButton');
    }

    function getCsrfToken() {
        var csrfMeta = document.querySelector("meta[name='_csrf']");
        return csrfMeta ? csrfMeta.getAttribute('content') : '';
    }

    function getContext() {
        var button = getButton();
        return button ? (button.getAttribute('data-recheck-context') || 'unsupported') : 'unsupported';
    }

    function buildBusyHtml(label) {
        return '<span class="recheck-pending-spinner" aria-hidden="true"></span><span class="recheck-pending-spinner-label">' + label + '</span>';
    }

    function buildNormalHtml() {
        return '<i class="ti-reload"></i> Re-check';
    }

    function setButtonState(options) {
        var button = getButton();
        if (!button) {
            return;
        }

        var disabled = !!options.disabled;
        var busy = !!options.busy;
        button.innerHTML = busy ? buildBusyHtml(options.busyLabel || 'Checking...') : buildNormalHtml();
        button.classList.toggle('recheck-pending-disabled', disabled || busy);
        button.style.pointerEvents = disabled || busy ? 'none' : '';
        button.style.opacity = disabled || busy ? '0.55' : '';
        button.setAttribute('aria-disabled', (disabled || busy) ? 'true' : 'false');
    }

    function ensureStyles() {
        if (document.getElementById('recheck-pending-styles')) {
            return;
        }
        var style = document.createElement('style');
        style.id = 'recheck-pending-styles';
        style.textContent = '' +
            '.recheck-pending-disabled { cursor: not-allowed !important; }' +
            '.recheck-pending-spinner {' +
            '  width: 12px;' +
            '  height: 12px;' +
            '  display: inline-block;' +
            '  vertical-align: -1px;' +
            '  margin-right: 6px;' +
            '  border: 2px solid currentColor;' +
            '  border-right-color: transparent;' +
            '  border-radius: 50%;' +
            '  box-sizing: border-box;' +
            '  animation: recheckPendingSpin 0.75s linear infinite;' +
            '}' +
            '.recheck-pending-spinner-label { display: inline-block; }' +
            '.recheck-pending-toast {' +
            '  position: fixed;' +
            '  top: 20px;' +
            '  right: 20px;' +
            '  z-index: 9999;' +
            '  background: #2c3e50;' +
            '  color: #fff;' +
            '  padding: 10px 14px;' +
            '  border-radius: 6px;' +
            '  box-shadow: 0 6px 18px rgba(0,0,0,0.18);' +
            '}' +
            '.recheck-summary-table { width: 100%; margin-bottom: 0; }' +
            '.recheck-summary-table th, .recheck-summary-table td { padding: 10px 12px !important; }' +
            '.recheck-summary-message { margin: 0 0 14px; color: #5f6f82; font-weight: 600; }' +
            '@keyframes recheckPendingSpin {' +
            '  from { transform: rotate(0deg); }' +
            '  to { transform: rotate(360deg); }' +
            '}';
        document.head.appendChild(style);
    }

    function showToast(message) {
        var toast = document.createElement('div');
        toast.className = 'recheck-pending-toast';
        toast.textContent = message;
        document.body.appendChild(toast);
        setTimeout(function() {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 2600);
    }

    function getSelectedTrackingNumbers() {
        if (!window.table || typeof window.table.getSelectedData !== 'function') {
            return [];
        }
        return window.table.getSelectedData()
            .filter(function(row) {
                return row && row.status === 'UNKNOWN' && row.code;
            })
            .map(function(row) {
                return row.code;
            });
    }

    function refreshCurrentTable() {
        if (!window.table) {
            return;
        }
        if (typeof window.table.setData === 'function') {
            window.table.setData();
        }
    }

    function ensureSummaryModal() {
        if (document.getElementById('recheck-pending-summary-modal')) {
            return;
        }
        var wrapper = document.createElement('div');
        wrapper.innerHTML = '' +
            '<div id="recheck-pending-summary-modal" class="modal fade" tabindex="-1" role="dialog">' +
            '  <div class="modal-dialog" role="document">' +
            '    <div class="modal-content">' +
            '      <div class="modal-header">' +
            '        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
            '        <h4 class="modal-title">Re-check summary</h4>' +
            '      </div>' +
            '      <div class="modal-body" id="recheck-pending-summary-body"></div>' +
            '      <div class="modal-footer">' +
            '        <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>' +
            '      </div>' +
            '    </div>' +
            '  </div>' +
            '</div>';
        document.body.appendChild(wrapper.firstChild);
    }

    function showSummaryModal(summary) {
        ensureSummaryModal();
        var body = document.getElementById('recheck-pending-summary-body');
        var messageHtml = summary && summary.message
            ? '<p class="recheck-summary-message">' + summary.message + '</p>'
            : '';
        body.innerHTML = '' +
            messageHtml +
            '<table class="table table-bordered recheck-summary-table">' +
            '  <thead>' +
            '    <tr><th>Metric</th><th>Count</th></tr>' +
            '  </thead>' +
            '  <tbody>' +
            '    <tr><td>Rechecked packages</td><td>' + (summary.recheckedCount || 0) + '</td></tr>' +
            '    <tr><td>Found from API</td><td>' + (summary.foundFromApiCount || 0) + '</td></tr>' +
            '    <tr><td>Found from file (DB)</td><td>' + (summary.foundFromDbCount || 0) + '</td></tr>' +
            '    <tr><td>Still missing</td><td>' + (summary.stillMissingCount || 0) + '</td></tr>' +
            '  </tbody>' +
            '</table>';
        $('#recheck-pending-summary-modal').modal('show');
    }

    function runRecheck(event) {
        if (event) {
            event.preventDefault();
        }
        if (requestInFlight) {
            return;
        }

        var context = getContext();
        if (context !== 'weight' && context !== 'scan-session') {
            showToast("This page doesn't support this action");
            return;
        }

        var selectedTrackingNumbers = getSelectedTrackingNumbers();
        var payload = {
            context: context,
            trackingNumbers: selectedTrackingNumbers
        };

        requestInFlight = true;
        setButtonState({busy: true, busyLabel: 'Checking...'});

        fetch('/recheck-pending', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': getCsrfToken()
            },
            body: JSON.stringify(payload)
        })
            .then(function(response) {
                if (!response.ok) {
                    return response.json().then(function(data) {
                        throw new Error(data && data.message ? data.message : 'Unable to re-check unknown packages');
                    }).catch(function() {
                        throw new Error('Unable to re-check unknown packages');
                    });
                }
                return response.json();
            })
            .then(function(data) {
                refreshCurrentTable();
                showSummaryModal(data || {});
            })
            .catch(function(error) {
                console.error(error);
                showToast(error && error.message ? error.message : 'Unable to re-check unknown packages');
            })
            .finally(function() {
                requestInFlight = false;
                setButtonState({disabled: context !== 'weight' && context !== 'scan-session'});
            });
    }

    document.addEventListener('DOMContentLoaded', function() {
        var button = getButton();
        if (!button) {
            return;
        }
        ensureStyles();
        setButtonState({disabled: getContext() !== 'weight' && getContext() !== 'scan-session'});
        button.addEventListener('click', runRecheck);
    });
})();
