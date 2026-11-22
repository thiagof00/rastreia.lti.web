(() => {
    const chave = 'rastreia-tema';
    const pagina = document.documentElement;

    try {
        if (localStorage.getItem(chave) === 'escuro') {
            pagina.dataset.tema = 'escuro';
        }
    } catch {
        // O tema também funciona se o navegador bloquear o armazenamento.
    }

    document.addEventListener('DOMContentLoaded', () => {
        const botao = document.getElementById('theme-toggle');
        const atualizarBotao = () => {
            botao.setAttribute('aria-pressed', String(pagina.dataset.tema === 'escuro'));
        };

        atualizarBotao();
        botao.hidden = false;
        botao.addEventListener('click', () => {
            const tema = pagina.dataset.tema === 'escuro' ? 'claro' : 'escuro';
            pagina.dataset.tema = tema;
            atualizarBotao();

            try {
                localStorage.setItem(chave, tema);
            } catch {
                // A escolha vale para esta página quando não puder ser salva.
            }
        });
    });
})();
