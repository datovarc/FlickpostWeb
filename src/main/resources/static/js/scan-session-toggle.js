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

    var scanSessionToggleRequestInFlight = false;
    function toggleScanSession(event) {
        if (event) {
            event.preventDefault();
        }
        if (scanSessionToggleRequestInFlight) {
            return;
        }
        scanSessionToggleRequestInFlight = true;

        fetch('/scan-session/session/toggle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': getCsrfToken()
            }
        })
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Unable to toggle scan session');
                }
                return response.json();
            })
            .then(function(data) {
                updateScanSessionMenuItem(data);
                if (data && data.active && window.location.pathname !== '/scan-session') {
                    window.location.href = '/scan-session';
                }
            })
            .catch(function(error) {
                console.error(error);
                alert('Unable to update scan session right now.');
            })
            .finally(function() {
                scanSessionToggleRequestInFlight = false;
            });
    }

    document.addEventListener('DOMContentLoaded', function() {
        var menuItems = document.querySelectorAll('#scanSessionToggleButton');
        if (!menuItems.length) {
            return;
        }

        loadScanSessionStatus();
        Array.prototype.forEach.call(menuItems, function(menuItem) {
            menuItem.addEventListener('click', toggleScanSession);
        });
    });
})();
