// Small helper: ask for confirmation before following links marked with data-confirm.
document.addEventListener('click', function (event) {
    var link = event.target.closest('[data-confirm]');
    if (link && !window.confirm(link.getAttribute('data-confirm'))) {
        event.preventDefault();
    }
});
