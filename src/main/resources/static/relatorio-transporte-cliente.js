(() => {
    const filtros = document.getElementById('transporte-filtros');
    const previa = document.getElementById('transporte-previa');
    const form = document.getElementById('transporte-form');
    const erro = document.getElementById('transporte-erro');
    const gerar = form.querySelector('[type="submit"]');
    const ver = document.getElementById('transporte-ver');
    const campos = form.elements;
    let clientesCarregados = false;
    let carregandoClientes = false;

    document.getElementById('transporte-gerar').addEventListener('click', async () => {
        erro.textContent = '';
        filtros.showModal();
        if (clientesCarregados || carregandoClientes) return;
        carregandoClientes = true;
        try {
            const resposta = await fetch(form.action + '/clientes');
            if (!resposta.ok) throw new Error();
            for (const cliente of await resposta.json()) {
                campos.cliente.add(new Option(cliente.nome, cliente.id));
            }
            clientesCarregados = true;
            gerar.disabled = false;
        } catch {
            erro.textContent = 'Não foi possível carregar os clientes. Feche a janela e tente novamente.';
        } finally {
            carregandoClientes = false;
        }
    });

    campos.periodo.addEventListener('change', () => {
        for (const [nome, mostrar] of [
            ['mes', campos.periodo.value === 'mes'],
            ['inicio', campos.periodo.value === 'intervalo'],
            ['fim', campos.periodo.value === 'intervalo']
        ]) {
            campos[nome].closest('label').hidden = !mostrar;
            campos[nome].disabled = !mostrar;
            campos[nome].required = mostrar;
        }
    });

    for (const janela of [filtros, previa]) {
        janela.querySelector('[data-fechar]').addEventListener('click', () => janela.close());
    }

    form.addEventListener('submit', async evento => {
        evento.preventDefault();
        erro.textContent = '';
        const dados = new FormData(form);
        const dataBr = valor => valor.split('-').reverse().join('/');
        if (dados.get('periodo') === 'intervalo' && dados.get('fim') < dados.get('inicio')) {
            erro.textContent = 'A data final deve ser igual ou posterior à inicial.';
            return;
        }
        const periodo = {
            todo: 'Todo o período',
            mes: dados.get('mes') ? dataBr(dados.get('mes')) : '',
            intervalo: dados.get('inicio') ? dataBr(dados.get('inicio')) + ' a ' + dataBr(dados.get('fim')) : ''
        }[dados.get('periodo')];
        const resumo = [
            campos.cliente.selectedOptions[0].text,
            periodo,
            'Status: ' + campos.status.selectedOptions[0].text,
            'Origem: ' + (campos.origem.value.trim() || 'Todas'),
            'Destino: ' + (campos.destino.value.trim() || 'Todos')
        ].join(' · ');

        gerar.disabled = true;
        try {
            const resposta = await fetch(form.action + '?' + new URLSearchParams(dados));
            if (!resposta.ok) throw new Error();
            const transportes = await resposta.json();
            const linhas = document.getElementById('transporte-linhas');
            linhas.replaceChildren();
            for (const transporte of transportes) {
                const status = [...campos.status.options].find(opcao => opcao.value === transporte.status);
                const linha = linhas.insertRow();
                for (const valor of [transporte.id, transporte.lti, transporte.cliente, transporte.criadoEm,
                    transporte.origem, transporte.destino, transporte.peso, status?.text ?? transporte.status]) {
                    linha.insertCell().textContent = valor ?? '—';
                }
            }
            const total = transportes.length ? transportes.length + ' transporte(s) encontrado(s)'
                : 'Nenhum transporte encontrado';
            document.getElementById('transporte-total').textContent = total;
            document.getElementById('transporte-quantidade').textContent = total;
            document.getElementById('transporte-resumo').textContent = resumo;
            ver.disabled = false;
            filtros.close();
        } catch {
            erro.textContent = 'Não foi possível gerar o relatório. Confira os filtros ou tente novamente.';
        } finally {
            gerar.disabled = false;
        }
    });

    ver.addEventListener('click', () => previa.showModal());
    document.getElementById('transporte-imprimir').addEventListener('click', () => {
        const janela = window.open('', '_blank');
        const aviso = document.getElementById('transporte-aviso-impressao');
        aviso.textContent = '';
        if (!janela) {
            aviso.textContent = 'Permita a abertura de janelas para imprimir ou salvar em PDF.';
            return;
        }
        janela.opener = null;
        janela.document.title = 'Transportes por cliente';
        janela.document.documentElement.lang = 'pt-BR';
        const estilo = janela.document.createElement('style');
        estilo.textContent = '@page{size:A4 landscape;margin:14mm}body{font:14px Arial;color:#111}' +
            'table{width:100%;border-collapse:collapse}th,td{padding:6px;border:1px solid #aaa;text-align:left}' +
            'thead{display:table-header-group}tr{break-inside:avoid}';
        janela.document.head.append(estilo);
        janela.document.body.append(janela.document.importNode(document.getElementById('transporte-impressao'), true));
        janela.focus();
        janela.print();
    });
})();
