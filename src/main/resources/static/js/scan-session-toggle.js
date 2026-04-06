(function() {
    var CACHE_KEY = 'scanSessionMenuState';
    var ACTIVE_BODY_CLASS = 'scan-session-active';
    var scanSessionRequestInFlight = false;

    function getCsrfToken() {
        var csrfMeta = document.querySelector("meta[name='_csrf']");
        return csrfMeta ? csrfMeta.getAttribute('content') : '';
    }

    function getMenuItems() {
        return document.querySelectorAll('#scanSessionToggleButton');
    }

    function getDefaultState() {
        return {
            active: false,
            buttonLabel: 'Start',
            iconClass: 'ti-control-play',
            busy: false,
            busyLabel: 'Starting...'
        };
    }

    function getCachedState() {
        try {
            var raw = window.localStorage.getItem(CACHE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (error) {
            console.error(error);
            return null;
        }
    }

    function setCachedState(state) {
        try {
            window.localStorage.setItem(CACHE_KEY, JSON.stringify({
                active: !!state.active,
                buttonLabel: state.buttonLabel,
                iconClass: state.iconClass,
                sessionId: state.sessionId || null,
                status: state.status || null,
                staffName: state.staffName || null,
                updatedAt: new Date().toISOString()
            }));
        } catch (error) {
            console.error(error);
        }
    }

    function clearCachedState() {
        try {
            window.localStorage.removeItem(CACHE_KEY);
        } catch (error) {
            console.error(error);
        }
    }

    function setMenuItemsDisabled(disabled) {
        Array.prototype.forEach.call(getMenuItems(), function(menuItem) {
            if (disabled) {
                menuItem.classList.add('scan-session-toggle-disabled');
                menuItem.setAttribute('aria-disabled', 'true');
                menuItem.setAttribute('data-busy', 'true');
                menuItem.style.pointerEvents = 'none';
                menuItem.style.opacity = '0.65';
            } else {
                menuItem.classList.remove('scan-session-toggle-disabled');
                menuItem.removeAttribute('aria-disabled');
                menuItem.setAttribute('data-busy', 'false');
                menuItem.style.pointerEvents = '';
                menuItem.style.opacity = '';
            }
        });
    }

    function buildBusyHtml(label) {
        return '<span class="scan-session-spinner" aria-hidden="true"></span><span class="scan-session-spinner-label">' + label + '</span>';
    }

    function buildLiveDotHtml() {
        return '<span class="scan-session-live-dot" aria-hidden="true"></span>';
    }

    function buildNormalHtml(iconClass, label, isActive) {
        var liveDot = isActive ? buildLiveDotHtml() : '';
        return liveDot + '<i class="ti ' + iconClass + '"></i> ' + label;
    }

    function updateBodySessionState(isActive) {
        if (!document.body) {
            return;
        }
        document.body.classList.toggle(ACTIVE_BODY_CLASS, !!isActive);
    }

    function updateScanSessionMenuItem(sessionState) {
        var menuItems = getMenuItems();
        var baseState = sessionState || getDefaultState();
        var buttonLabel = baseState.buttonLabel || 'Start';
        var iconClass = baseState.iconClass || 'ti-control-play';
        var isBusy = baseState.busy === true;
        var isActive = baseState.active === true;
        var html = isBusy
            ? buildBusyHtml(baseState.busyLabel || 'Working...')
            : buildNormalHtml(iconClass, buttonLabel, isActive);

        updateBodySessionState(isActive);

        if (!menuItems.length) {
            return;
        }

        Array.prototype.forEach.call(menuItems, function(menuItem) {
            menuItem.innerHTML = html;
            menuItem.setAttribute('data-active', isActive ? 'true' : 'false');
        });

        setMenuItemsDisabled(isBusy);
    }

    function buildOptimisticState(isActive) {
        if (isActive) {
            return {
                active: true,
                buttonLabel: 'Stop',
                iconClass: 'ti-control-stop'
            };
        }
        return {
            active: false,
            buttonLabel: 'Start',
            iconClass: 'ti-control-play'
        };
    }

    function applyCachedStateImmediately() {
        var cachedState = getCachedState();
        if (cachedState) {
            updateScanSessionMenuItem(cachedState);
        } else {
            updateScanSessionMenuItem(getDefaultState());
        }
    }

    function loadScanSessionStatus() {
        return fetch('/scan-session/session/status', {
            method: 'GET',
            headers: {
                'X-CSRF-Token': getCsrfToken()
            }
        })
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Unable to load scan session status');
                }
                return response.json();
            })
            .then(function(data) {
                if (data && data.active) {
                    setCachedState(data);
                } else {
                    clearCachedState();
                }
                updateScanSessionMenuItem(data || getDefaultState());
                return data;
            })
            .catch(function(error) {
                console.error(error);
            });
    }

    function ensureConfirmationModal() {
        if (document.getElementById('scan-session-confirmation-modal')) {
            return;
        }

        var modal = document.createElement('div');
        modal.id = 'scan-session-confirmation-modal';
        modal.className = 'modal fade';
        modal.setAttribute('tabindex', '-1');
        modal.setAttribute('role', 'dialog');
        modal.innerHTML = '' +
            '<div class="modal-dialog scan-session-confirmation-dialog" role="document">' +
            '  <div class="modal-content">' +
            '    <div class="modal-header">' +
            '      <h4 class="modal-title text-center" id="scan-session-confirmation-title">Confirm scan session action</h4>' +
            '    </div>' +
            '    <div class="modal-body">' +
            '      <p id="scan-session-confirmation-message" class="text-center" style="margin-bottom:0;">Are you sure?</p>' +
            '    </div>' +
            '    <div class="modal-footer">' +
            '      <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>' +
            '      <button type="button" class="btn btn-primary" id="scan-session-confirmation-continue">Continue</button>' +
            '    </div>' +
            '  </div>' +
            '</div>';
        document.body.appendChild(modal);
    }

    function confirmScanSessionAction(isActive) {
        ensureConfirmationModal();

        return new Promise(function(resolve) {
            var modal = $('#scan-session-confirmation-modal');
            var continueButton = document.getElementById('scan-session-confirmation-continue');
            var title = document.getElementById('scan-session-confirmation-title');
            var message = document.getElementById('scan-session-confirmation-message');
            var resolved = false;

            title.textContent = isActive ? 'Stop scanning session?' : 'Start scanning session?';
            message.textContent = isActive
                ? 'You are about to stop the current scanning session.'
                : 'You are about to start a new scanning session.';

            function cleanup(result) {
                if (resolved) {
                    return;
                }
                resolved = true;
                continueButton.removeEventListener('click', onContinue);
                modal.off('hidden.bs.modal', onHidden);
                resolve(result);
            }

            function onContinue() {
                cleanup(true);
                modal.modal('hide');
            }

            function onHidden() {
                cleanup(false);
            }

            continueButton.addEventListener('click', onContinue);
            modal.on('hidden.bs.modal', onHidden);
            modal.modal({backdrop: 'static', keyboard: true});
            modal.modal('show');
        });
    }

    function handleScanSessionClick(event) {
        if (event) {
            event.preventDefault();
        }
        if (scanSessionRequestInFlight) {
            return;
        }

        var toggleButton = document.querySelector('#scanSessionToggleButton');
        var isActive = toggleButton && toggleButton.getAttribute('data-active') === 'true';
        var endpoint = isActive ? '/scan-session/session/stop' : '/scan-session/session/start';
        var busyLabel = isActive ? 'Stopping...' : 'Starting...';
        var optimisticState = buildOptimisticState(!isActive);

        confirmScanSessionAction(isActive).then(function(confirmed) {
            if (!confirmed) {
                return;
            }

            scanSessionRequestInFlight = true;
            updateScanSessionMenuItem({
                active: isActive,
                buttonLabel: isActive ? 'Stop' : 'Start',
                iconClass: isActive ? 'ti-control-stop' : 'ti-control-play',
                busy: true,
                busyLabel: busyLabel
            });

            if (!isActive) {
                setCachedState(optimisticState);
                window.scanSessionIsActive = true;
                if (typeof window.updateAddStatusActionState === 'function') {
                    window.updateAddStatusActionState();
                }
            }

            fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-Token': getCsrfToken()
                }
            })
                .then(function(response) {
                    if (!response.ok) {
                        throw new Error('Unable to update scan session');
                    }
                    return response.json();
                })
                .then(function(data) {
                    if (data && data.active) {
                        setCachedState(data);
                    } else {
                        clearCachedState();
                    }
                    updateScanSessionMenuItem(data || getDefaultState());
                    if (!isActive && window.location.pathname !== '/scan-session') {
                        window.location.href = '/scan-session';
                    }
                })
                .catch(function(error) {
                    console.error(error);
                    loadScanSessionStatus();
                    alert('Unable to update scan session right now.');
                })
                .finally(function() {
                    scanSessionRequestInFlight = false;
                });
        });
    }

    function injectMenuFeedbackStyles() {
        if (document.getElementById('scan-session-toggle-styles')) {
            return;
        }

        var style = document.createElement('style');
        style.id = 'scan-session-toggle-styles';
        style.textContent = '' +
            '.scan-session-toggle-disabled {' +
            '  cursor: not-allowed !important;' +
            '}' +
            '.scan-session-spinner {' +
            '  width: 12px;' +
            '  height: 12px;' +
            '  display: inline-block;' +
            '  vertical-align: -1px;' +
            '  margin-right: 6px;' +
            '  border: 2px solid currentColor;' +
            '  border-right-color: transparent;' +
            '  border-radius: 50%;' +
            '  box-sizing: border-box;' +
            '  animation: scanSessionSpin 0.75s linear infinite;' +
            '  transform-origin: 50% 50%;' +
            '}' +
            '.scan-session-spinner-label {' +
            '  display: inline-block;' +
            '}' +
            '.scan-session-live-dot {' +
            '  width: 8px;' +
            '  height: 8px;' +
            '  display: inline-block;' +
            '  margin-right: 8px;' +
            '  border-radius: 50%;' +
            '  background: #e53935;' +
            '  vertical-align: 2px;' +
            '  animation: scanSessionLiveFade 1.8s ease-in-out infinite;' +
            '}' +
            '.scan-session-confirmation-dialog {' +
            '  width: 420px;' +
            '  max-width: calc(100% - 30px);' +
            '  margin: 90px auto;' +
            '}' +
            '.scan-session-confirmation-dialog .modal-content {' +
            '  border-radius: 8px;' +
            '}' +
            '.scan-session-confirmation-dialog .modal-body {' +
            '  padding: 28px 26px 30px;' +
            '}' +
            '.scan-session-confirmation-dialog .modal-footer {' +
            '  border-top: 0;' +
            '  padding: 0 26px 24px;' +
            '}' +
            '.scan-session-confirmation-dialog .modal-header {' +
            '  padding: 20px 26px 10px;' +
            '  border-bottom: 0;' +
            '}' +
            '.scan-session-confirmation-dialog .modal-title {' +
            '  font-size: 20px;' +
            '}' +
            'body.scan-session-active {' +
            '  background: #3A3D41;' +
            '}' +
            '@keyframes scanSessionSpin {' +
            '  from { transform: rotate(0deg); }' +
            '  to { transform: rotate(360deg); }' +
            '}' +
            '@keyframes scanSessionLiveFade {' +
            '  0% { opacity: 0; }' +
            '  50% { opacity: 1; }' +
            '  100% { opacity: 0; }' +
            '}';
        document.head.appendChild(style);
    }

    document.addEventListener('DOMContentLoaded', function() {
        var menuItems = getMenuItems();
        if (!menuItems.length) {
            return;
        }

        injectMenuFeedbackStyles();
        applyCachedStateImmediately();
        loadScanSessionStatus();

        Array.prototype.forEach.call(menuItems, function(menuItem) {
            menuItem.addEventListener('click', handleScanSessionClick);
        });
    });
})();
