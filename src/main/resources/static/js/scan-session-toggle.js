(function() {
    function getCsrfToken() {
        var csrfMeta = document.querySelector("meta[name='_csrf']");
        return csrfMeta ? csrfMeta.getAttribute('content') : '';
    }

    function updateScanSessionMenuItem(sessionState) {
        var menuItems = document.querySelectorAll('#scanSessionToggleButton');
        if (!menuItems.length) {
            return;
        }

        var buttonLabel = sessionState && sessionState.buttonLabel ? sessionState.buttonLabel : 'Start';
        var iconClass = sessionState && sessionState.iconClass ? sessionState.iconClass : 'ti-control-play';
        Array.prototype.forEach.call(menuItems, function(menuItem) {
            menuItem.innerHTML = '<i class="ti ' + iconClass + '"></i> ' + buttonLabel;
            menuItem.setAttribute('data-active', sessionState && sessionState.active ? 'true' : 'false');
        });
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
                updateScanSessionMenuItem(data);
                return data;
            })
            .catch(function(error) {
                console.error(error);
            });
    }

    var scanSessionRequestInFlight = false;
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
        scanSessionRequestInFlight = true;

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
                updateScanSessionMenuItem(data);
                if (!isActive && window.location.pathname !== '/scan-session') {
                    window.location.href = '/scan-session';
                }
            })
            .catch(function(error) {
                console.error(error);
                alert('Unable to update scan session right now.');
            })
            .finally(function() {
                scanSessionRequestInFlight = false;
            });
    }

    document.addEventListener('DOMContentLoaded', function() {
        var menuItems = document.querySelectorAll('#scanSessionToggleButton');
        if (!menuItems.length) {
            return;
        }

        loadScanSessionStatus();
        Array.prototype.forEach.call(menuItems, function(menuItem) {
            menuItem.addEventListener('click', handleScanSessionClick);
        });
    });
})();
