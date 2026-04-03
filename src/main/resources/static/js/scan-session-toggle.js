(function() {
    var CACHE_KEY = 'scanSessionMenuState';
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

    function buildNormalHtml(iconClass, label) {
        return '<i class="ti ' + iconClass + '"></i> ' + label;
    }

    function updateScanSessionMenuItem(sessionState) {
        var menuItems = getMenuItems();
        if (!menuItems.length) {
            return;
        }

        var baseState = sessionState || getDefaultState();
        var buttonLabel = baseState.buttonLabel || 'Start';
        var iconClass = baseState.iconClass || 'ti-control-play';
        var isBusy = baseState.busy === true;
        var html = isBusy
            ? buildBusyHtml(baseState.busyLabel || 'Working...')
            : buildNormalHtml(iconClass, buttonLabel);

        Array.prototype.forEach.call(menuItems, function(menuItem) {
            menuItem.innerHTML = html;
            menuItem.setAttribute('data-active', baseState.active ? 'true' : 'false');
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
            '@keyframes scanSessionSpin {' +
            '  from { transform: rotate(0deg); }' +
            '  to { transform: rotate(360deg); }' +
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
