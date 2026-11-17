/* BookSwap front-end behaviour: navigation, confirmations, loading state, password checks. */
(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    // --- mobile navigation ---
    var menuBtn = document.getElementById('menuBtn');
    var sidebar = document.getElementById('sidebar');
    if (menuBtn && sidebar) {
      menuBtn.addEventListener('click', function () { sidebar.classList.toggle('open'); });
      document.addEventListener('click', function (e) {
        if (sidebar.classList.contains('open') && !sidebar.contains(e.target) && e.target !== menuBtn) {
          sidebar.classList.remove('open');
        }
      });
    }

    // --- notifications ---
    var flash = document.querySelector('.flash');
    if (flash) {
      var close = flash.querySelector('.flash-close');
      if (close) { close.addEventListener('click', function () { flash.remove(); }); }
      if (flash.classList.contains('flash-success')) {
        setTimeout(function () { if (flash.parentNode) { flash.remove(); } }, 6000);
      }
    }

    // --- confirmation before destructive actions: <form data-confirm="..."> ---
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
      form.addEventListener('submit', function (e) {
        if (!window.confirm(form.getAttribute('data-confirm'))) { e.preventDefault(); }
      });
    });

    // --- <select class="auto-submit"> saves on change ---
    document.querySelectorAll('select.auto-submit').forEach(function (sel) {
      sel.addEventListener('change', function () { sel.form.submit(); });
    });

    // --- password confirmation: <form data-match="passwordId,confirmId"> ---
    document.querySelectorAll('form[data-match]').forEach(function (form) {
      var ids = form.getAttribute('data-match').split(',');
      var a = form.querySelector('#' + ids[0]);
      var b = form.querySelector('#' + ids[1]);
      if (!a || !b) { return; }
      function check() {
        b.setCustomValidity(a.value !== b.value ? 'Passwords do not match.' : '');
      }
      a.addEventListener('input', check);
      b.addEventListener('input', check);
      form.addEventListener('submit', check);
    });

    // --- loading state on submit buttons (runs after validation and confirmation) ---
    document.querySelectorAll('form').forEach(function (form) {
      form.addEventListener('submit', function (e) {
        if (e.defaultPrevented) { return; }
        form.querySelectorAll('button[type="submit"]').forEach(function (btn) {
          btn.classList.add('is-loading');
          setTimeout(function () { btn.disabled = true; }, 0);
        });
      });
    });

    // --- messages passed in the URL by the servlets (login / register pages) ---
    var notice = document.getElementById('notice');
    if (notice) {
      var params = new URLSearchParams(window.location.search);
      var type = params.get('error') ? 'error' : (params.get('success') ? 'success' : null);
      if (type) {
        notice.textContent = params.get(type);   // textContent: never interpreted as HTML
        notice.className = 'notice show ' + type;
      }
    }
  });

  // Back/forward cache: make sure buttons are usable again.
  window.addEventListener('pageshow', function () {
    document.querySelectorAll('button.is-loading').forEach(function (btn) {
      btn.classList.remove('is-loading');
      btn.disabled = false;
    });
  });
})();
