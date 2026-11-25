const formularioBusca = document.getElementById('busca-lista');
const campoBusca = formularioBusca.elements.busca;
const resultadosBusca = document.getElementById('resultados-busca');
let esperaBusca;
let requisicaoBusca;

campoBusca.addEventListener('input', () => {
    clearTimeout(esperaBusca);
    requisicaoBusca?.abort();
    esperaBusca = setTimeout(atualizarResultados, 250);
});

async function atualizarResultados() {
    const url = new URL(formularioBusca.action);
    const busca = campoBusca.value.trim();
    if (busca) url.searchParams.set('busca', busca);

    const requisicao = new AbortController();
    requisicaoBusca = requisicao;
    try {
        const resposta = await fetch(url, { signal: requisicao.signal });
        if (!resposta.ok) throw new Error('Falha ao buscar registros');

        const html = await resposta.text();
        if (requisicao.signal.aborted) return;
        const pagina = new DOMParser().parseFromString(html, 'text/html');
        const resultados = pagina.getElementById('resultados-busca');
        if (!resultados) throw new Error('Resultados indisponíveis');

        resultadosBusca.replaceChildren(...resultados.childNodes);
        history.replaceState(history.state, '', url);
    } catch (erro) {
        if (requisicao.signal.aborted) return;
        resultadosBusca.querySelector('.pagination-info').textContent =
            'Falha ao atualizar a busca. Pressione Enter para tentar novamente.';
    }
}
