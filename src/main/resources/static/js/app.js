// Buscador del topbar: filtra los elementos .item-buscable por texto.
document.addEventListener('DOMContentLoaded', function () {
    var buscador = document.getElementById('buscador');
    if (buscador) {
        buscador.addEventListener('input', function () {
            var q = buscador.value.toLowerCase().trim();
            document.querySelectorAll('.item-buscable').forEach(function (el) {
                el.style.display = el.textContent.toLowerCase().includes(q) ? '' : 'none';
            });
        });
    }

    // Tabs de filtro: <button data-filtro="PENDIENTE"> filtra elementos con data-estado.
    document.querySelectorAll('.tabs-sf button[data-filtro]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            btn.parentElement.querySelectorAll('button').forEach(function (b) { b.classList.remove('active'); });
            btn.classList.add('active');
            var f = btn.dataset.filtro;
            document.querySelectorAll('[data-estado]').forEach(function (el) {
                el.style.display = (f === 'TODOS' || el.dataset.estado === f) ? '' : 'none';
            });
        });
    });

    // Tabs de contenido (detalle de proyecto): <button data-panel="panel-x">
    var tabs = document.querySelectorAll('.tabs-sf button[data-panel]');
    function abrir(id) {
        tabs.forEach(function (b) { b.classList.toggle('active', b.dataset.panel === id); });
        document.querySelectorAll('.panel-tab').forEach(function (p) { p.classList.toggle('d-none', p.id !== id); });
    }
    tabs.forEach(function (b) { b.addEventListener('click', function () { abrir(b.dataset.panel); }); });
    var t = new URLSearchParams(location.search).get('tab');
    if (tabs.length) abrir(t && document.getElementById('panel-' + t) ? 'panel-' + t : 'panel-resumen');
});
