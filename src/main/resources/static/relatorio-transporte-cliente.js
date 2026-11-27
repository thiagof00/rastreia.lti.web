(() => {
    const filtros = document.getElementById('transporte-filtros');
    const previa = document.getElementById('transporte-previa');
    const form = document.getElementById('transporte-form');
    const erro = document.getElementById('transporte-erro');
    const gerar = form.querySelector('[type="submit"]');
    const campos = form.elements;
    const resultados = new Map();
    const tabelas = {
        invoices: 'transporte',
        documentacao: 'transporte',
        combinacoes: 'direta',
        peso: 'periodo',
        pendencias: 'periodo',
        sessoes: 'direta',
        bloqueios: 'direta',
        timeline: 'transporte',
        produtividade: 'periodo',
        utilizacao: 'periodo',
        status: 'periodo',
        ocupacao: 'direta'
    };
    const selecoes = {
        motorista: { campo: campos.motoristaId, lista: '/motoristas', consulta: '/etapas-motorista' },
        caminhao: { campo: campos.caminhaoId, lista: '/caminhoes', consulta: '/etapas-caminhao' },
        carreta: { campo: campos.carretaId, lista: '/carretas', consulta: '/etapas-carreta' }
    };
    let cartaoAtual;
    let gerando = false;
    let clientesCarregados = false;
    let carregandoClientes = false;

    async function abrirFiltros(cartao) {
        if (gerando) return;
        if (cartaoAtual !== cartao) {
            form.reset();
            campos.periodo.dispatchEvent(new Event('change'));
        }
        cartaoAtual = cartao;
        const porCliente = cartao.dataset.transporte === 'cliente';
        const tipo = cartao.dataset.transporte;
        const tabela = tabelas[tipo];
        const porEtapas = ['etapas', 'pedidos', 'nfe'].includes(tipo) || tabela === 'transporte';
        const consultaDireta = tabela === 'direta';
        const selecao = selecoes[tipo];
        for (const [chave, { campo }] of Object.entries(selecoes)) {
            const visivel = chave === tipo;
            campo.closest('label').hidden = !visivel;
            campo.disabled = !visivel;
            campo.required = visivel;
        }
        campos.periodo.querySelector('[value="mes"]').hidden = Boolean(selecao) || tabela === 'periodo';
        for (const nome of ['status', 'origem', 'destino']) {
            campos[nome].closest('label').hidden = Boolean(selecao || tabela);
        }
        form.querySelector('[data-explicacao-periodo]').textContent = selecao || ['produtividade', 'utilizacao'].includes(tipo)
            ? 'O período considera a data de criação da etapa.'
            : 'O período considera a data de criação do transporte.';
        form.querySelector('.fields-grid').hidden = porEtapas || consultaDireta;
        form.querySelector('[data-explicacao-periodo]').hidden = porEtapas || consultaDireta;
        campos.transporteId.closest('label').hidden = !porEtapas;
        campos.transporteId.disabled = !porEtapas;
        campos.transporteId.required = porEtapas;
        gerar.textContent = porEtapas || selecao || tabela ? 'Consultar' : 'Gerar relatório';
        campos.cliente.disabled = !porCliente;
        campos.cliente.closest('label').hidden = !porCliente;
        gerar.disabled = (porCliente && !clientesCarregados) || (selecao && !selecao.campo.dataset.carregado);
        filtros.querySelector('h2').textContent = cartao.querySelector('h3').textContent;
        erro.textContent = '';
        if (consultaDireta) {
            form.requestSubmit();
            return;
        }
        filtros.showModal();
        if (selecao && !selecao.campo.dataset.carregado) {
            try {
                const resposta = await fetch(form.action + selecao.lista);
                if (!resposta.ok) throw new Error();
                const opcoes = await resposta.json();
                selecao.campo.options.length = 1;
                for (const opcao of opcoes) {
                    selecao.campo.add(new Option(opcao.nome, opcao.id));
                }
                selecao.campo.dataset.carregado = 'true';
                if (cartaoAtual === cartao && !gerando) gerar.disabled = false;
            } catch {
                if (cartaoAtual === cartao) erro.textContent = 'Não foi possível carregar as opções.';
            }
            return;
        }
        if (!porCliente || clientesCarregados || carregandoClientes) return;
        carregandoClientes = true;
        try {
            const resposta = await fetch(form.action + '/clientes');
            if (!resposta.ok) throw new Error();
            for (const cliente of await resposta.json()) {
                campos.cliente.add(new Option(cliente.nome, cliente.id));
            }
            clientesCarregados = true;
            if (!gerando) gerar.disabled = false;
        } catch {
            if (cartaoAtual === cartao) {
                erro.textContent = 'Não foi possível carregar os clientes. Feche a janela e tente novamente.';
            }
        } finally {
            carregandoClientes = false;
        }
    }

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
        const tipo = cartaoAtual.dataset.transporte;
        const tabela = tabelas[tipo];
        const consultaDireta = tabela === 'direta';
        const porPedidos = tipo === 'pedidos';
        const porNfe = tipo === 'nfe';
        const porEtapas = tipo === 'etapas' || porPedidos || porNfe || tabela === 'transporte';
        const selecao = selecoes[cartaoAtual.dataset.transporte];
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
        const resumo = porEtapas ? 'Transporte #' + dados.get('transporteId')
            : tabela ? (tabela === 'periodo' ? periodo : '') : selecao
            ? selecao.campo.selectedOptions[0].text + ' · ' + periodo : [
            dados.has('cliente') ? campos.cliente.selectedOptions[0].text : '',
            periodo,
            'Status: ' + campos.status.selectedOptions[0].text,
            'Origem: ' + (campos.origem.value.trim() || 'Todas'),
            'Destino: ' + (campos.destino.value.trim() || 'Todos')
        ].filter(Boolean).join(' · ');

        gerando = true;
        gerar.disabled = true;
        const mensagemCartao = cartaoAtual.querySelector('[data-transporte-total]');
        if (consultaDireta) mensagemCartao.textContent = 'Consultando…';
        try {
            let url = porEtapas
                ? form.action + '/' + tipo + '?' + new URLSearchParams({ transporteId: dados.get('transporteId') })
                : form.action + '?' + new URLSearchParams(dados);
            if (selecao) {
                const parametros = new URLSearchParams({ [selecao.campo.name]: dados.get(selecao.campo.name) });
                if (dados.get('periodo') === 'intervalo') {
                    parametros.set('inicio', dados.get('inicio'));
                    parametros.set('fim', dados.get('fim'));
                }
                url = form.action + selecao.consulta + '?' + parametros;
            }
            if (tabela) {
                const parametros = new URLSearchParams();
                if (tabela === 'transporte') parametros.set('transporteId', dados.get('transporteId'));
                if (tabela === 'periodo' && dados.get('periodo') === 'intervalo') {
                    parametros.set('inicio', dados.get('inicio'));
                    parametros.set('fim', dados.get('fim'));
                }
                url = form.action + '/tabelas/' + tipo + '?' + parametros;
            }
            const resposta = await fetch(url);
            if (resposta.status === 403) {
                (consultaDireta ? mensagemCartao : erro).textContent = 'Seu perfil não tem permissão para esta consulta.';
                return;
            }
            if (porEtapas && resposta.status === 404) {
                erro.textContent = 'Transporte não encontrado. Confira o ID informado.';
                return;
            }
            if (!resposta.ok) throw new Error();
            const payload = await resposta.json();
            const transportes = tabela ? payload.linhas : payload;
            const total = tabela
                ? (transportes.length ? transportes.length + ' registro(s) encontrado(s)' : 'Nenhum registro encontrado')
                : porNfe
                ? (transportes.length ? transportes.length + ' NF-e vinculada(s)' : 'Nenhuma NF-e vinculada')
                : porPedidos
                ? (transportes.length ? transportes.length + ' pedido(s) encontrado(s)' : 'Nenhum pedido vinculado')
                : porEtapas || selecao
                ? (transportes.length ? transportes.length + ' etapa(s) encontrada(s)' : 'Nenhuma etapa encontrada')
                : (transportes.length ? transportes.length + ' transporte(s) encontrado(s)' : 'Nenhum transporte encontrado');
            resultados.set(cartaoAtual, { transportes, resumo, total, colunas: tabela ? payload.colunas : null });
            mensagemCartao.textContent = total;
            cartaoAtual.querySelector('[data-transporte-ver]').disabled = false;
            if (!consultaDireta) filtros.close();
            if (consultaDireta || ['timeline', 'produtividade', 'utilizacao', 'status'].includes(tipo)) {
                abrirPrevia(cartaoAtual);
            }
        } catch {
            (consultaDireta ? mensagemCartao : erro).textContent =
                'Não foi possível concluir a consulta. Tente novamente.';
        } finally {
            gerando = false;
            gerar.disabled = false;
        }
    });

    function abrirPrevia(cartao) {
        const { transportes, resumo, total, colunas } = resultados.get(cartao);
        const porEtapas = cartao.dataset.transporte === 'etapas';
        const porPedidos = cartao.dataset.transporte === 'pedidos';
        const porNfe = cartao.dataset.transporte === 'nfe';
        const tipo = cartao.dataset.transporte;
        const selecao = selecoes[tipo];
        previa.querySelector('[data-colunas-motorista]').hidden = !selecao;
        previa.querySelector('[data-colunas-transportes]').hidden =
            porEtapas || porPedidos || porNfe || Boolean(selecao || colunas);
        previa.querySelector('[data-colunas-etapas]').hidden = !porEtapas;
        previa.querySelector('[data-colunas-pedidos]').hidden = !porPedidos;
        previa.querySelector('[data-colunas-nfe]').hidden = !porNfe;
        const cabecalho = previa.querySelector('[data-colunas-tabela]');
        cabecalho.hidden = !colunas;
        cabecalho.replaceChildren();
        for (const coluna of colunas ?? []) {
            const celula = document.createElement('th');
            celula.scope = 'col';
            celula.textContent = coluna;
            cabecalho.append(celula);
        }
        const linhas = document.getElementById('transporte-linhas');
        linhas.replaceChildren();
        for (const transporte of transportes) {
            const status = [...campos.status.options].find(opcao => opcao.value === transporte.status);
            const linha = linhas.insertRow();
            const valores = colunas ? transporte : porNfe
                ? [transporte.id, transporte.numero, transporte.serie]
                : porPedidos
                ? [transporte.id, transporte.numero, transporte.serie,
                    transporte.dataEmissao?.split('-').reverse().join('/')]
                : selecao
                ? [transporte.id, transporte.transporteId, transporte.observacoes, transporte.criadoEm,
                    status?.text ?? transporte.status]
                : porEtapas
                ? [transporte.id, transporte.observacoes, transporte.criadoEm, transporte.atualizadoEm]
                : [transporte.id, transporte.lti, transporte.cliente, transporte.criadoEm,
                    transporte.origem, transporte.destino, transporte.peso, status?.text ?? transporte.status];
            for (const valor of valores) {
                linha.insertCell().textContent = valor ?? '—';
            }
        }
        document.getElementById('transporte-quantidade').textContent = total;
        renderizarVisualizacao(tipo, transportes);
        document.getElementById('transporte-resumo').textContent = resumo;
        document.getElementById('transporte-aviso-impressao').textContent = '';
        previa.querySelector('h2').textContent = cartao.querySelector('h3').textContent;
        previa.showModal();
    }

    function renderizarVisualizacao(tipo, registros) {
        const area = document.getElementById('transporte-visualizacao');
        area.replaceChildren();
        if (!registros.length) return;

        if (tipo === 'timeline') {
            const painel = document.createElement('div');
            painel.className = 'transporte-timeline-painel';
            const lista = document.createElement('ol');
            lista.className = 'transporte-timeline';
            for (const [data, observacoes, motorista, caminhao, carreta] of registros) {
                const etapa = document.createElement('li');
                const titulo = document.createElement('strong');
                titulo.textContent = data ?? 'Data indisponível';
                const texto = document.createElement('p');
                texto.textContent = observacoes || 'Sem observações';
                const detalhes = document.createElement('small');
                detalhes.textContent = [
                    motorista && 'Motorista: ' + motorista,
                    caminhao && 'Caminhão: ' + caminhao,
                    carreta && 'Carreta: ' + carreta
                ].filter(Boolean).join(' · ');
                etapa.append(titulo, texto, detalhes);
                lista.append(etapa);
            }
            painel.append(lista, criarGraficoTimeline(registros));
            area.append(painel);
            return;
        }

        const numero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
        if (tipo === 'status') {
            const cores = ['#526fe5', '#087f72', '#b65416', '#a63878'];
            const total = registros.reduce((soma, [, quantidade]) => soma + Number(quantidade), 0);
            if (total === 0) return;
            const legenda = document.createElement('ul');
            legenda.className = 'transporte-legenda';
            const setores = [];
            let inicio = 0;
            registros.forEach(([status, quantidade], indice) => {
                const percentual = Number(quantidade) / total * 100;
                const cor = cores[indice % cores.length];
                setores.push(cor + ' ' + inicio + '% ' + (inicio + percentual) + '%');
                inicio += percentual;
                const item = document.createElement('li');
                item.style.borderInlineStartColor = cor;
                const nome = [...campos.status.options].find(opcao => opcao.value === status)?.text ?? status;
                item.textContent = nome + ' · ' + numero.format(quantidade) + ' (' + numero.format(percentual) + '%)';
                legenda.append(item);
            });
            const pizza = document.createElement('div');
            pizza.className = 'transporte-pizza';
            pizza.style.backgroundImage = 'conic-gradient(' + setores.join(', ') + ')';
            pizza.setAttribute('aria-hidden', 'true');
            area.append(pizza, legenda);
            return;
        }

        if (tipo === 'produtividade') {
            renderizarProdutividade(area, registros, numero);
            return;
        }
        if (tipo !== 'utilizacao') return;
        const indiceEtapas = 2;
        const maiores = registros.slice(0, 10);
        const maximo = Math.max(1, ...maiores.map(linha => Number(linha[indiceEtapas])));
        const titulo = document.createElement('h3');
        titulo.textContent = 'Etapas registradas · até 10 maiores contagens';
        const lista = document.createElement('ol');
        lista.className = 'transporte-barras';
        for (const linha of maiores) {
            const item = document.createElement('li');
            const rotulo = document.createElement('span');
            const etapas = Number(linha[indiceEtapas]);
            const nome = linha[0] + ' · ' + linha[1];
            rotulo.textContent = nome + ' · ' + numero.format(etapas);
            const barra = document.createElement('progress');
            barra.max = maximo;
            barra.value = etapas;
            barra.setAttribute('aria-hidden', 'true');
            item.append(rotulo, barra);
            lista.append(item);
        }
        area.append(titulo, lista);
    }

    function criarGraficoTimeline(registros) {
        const figura = document.createElement('figure');
        figura.className = 'transporte-timeline-grafico';
        const titulo = document.createElement('figcaption');
        titulo.textContent = 'Etapas ao longo do tempo';
        figura.append(titulo);

        const pontos = registros.flatMap(([data, observacao], indice) => {
            const partes = data?.match(/^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})$/);
            if (!partes) return [];
            const [, dia, mes, ano, hora, minuto] = partes;
            return [{ data, observacao, etapa: indice + 1, instante: Date.UTC(ano, mes - 1, dia, hora, minuto) }];
        });
        if (!pontos.length) {
            figura.append('Nenhuma etapa com data disponível.');
            return figura;
        }

        const namespace = 'http://www.w3.org/2000/svg';
        const svg = document.createElementNS(namespace, 'svg');
        svg.setAttribute('viewBox', '0 0 520 230');
        svg.setAttribute('role', 'img');
        svg.setAttribute('aria-label', 'Gráfico de linha com data e hora no eixo horizontal e número da etapa no eixo vertical');
        const desenho = (tipo, atributos, texto) => {
            const elemento = document.createElementNS(namespace, tipo);
            for (const [nome, valor] of Object.entries(atributos)) elemento.setAttribute(nome, valor);
            if (texto) elemento.textContent = texto;
            svg.append(elemento);
            return elemento;
        };
        const primeiro = pontos[0];
        const ultimo = pontos.at(-1);
        const duracao = ultimo.instante - primeiro.instante;
        const coordenadas = pontos.map(ponto => ({
            ...ponto,
            x: duracao ? 56 + (ponto.instante - primeiro.instante) / duracao * 430 : 271,
            y: 168 - (ponto.etapa - 1) / Math.max(1, registros.length - 1) * 135
        }));

        desenho('path', { d: 'M 48 24 V 178 H 494', class: 'timeline-eixos' });
        desenho('text', { x: 48, y: 15 }, 'Etapa');
        desenho('text', { x: 39, y: 172, 'text-anchor': 'end' }, '1');
        if (registros.length > 1) {
            desenho('text', { x: 39, y: 37, 'text-anchor': 'end' }, String(registros.length));
        }
        desenho('text', { x: 56, y: 198 }, primeiro.data);
        if (duracao) desenho('text', { x: 486, y: 198, 'text-anchor': 'end' }, ultimo.data);
        desenho('text', { x: 271, y: 223, 'text-anchor': 'middle' }, 'Data e hora');
        desenho('polyline', {
            points: coordenadas.map(({ x, y }) => x + ',' + y).join(' '),
            class: 'timeline-trajeto'
        });
        for (const ponto of coordenadas) {
            const circulo = desenho('circle', { cx: ponto.x, cy: ponto.y, r: 5, class: 'timeline-ponto' });
            const detalhe = document.createElementNS(namespace, 'title');
            detalhe.textContent = 'Etapa ' + ponto.etapa + ' · ' + ponto.data
                + (ponto.observacao ? ' · ' + ponto.observacao : '');
            circulo.append(detalhe);
        }
        figura.append(svg);
        return figura;
    }

    function renderizarProdutividade(area, registros, numero) {
        const maiores = registros.slice(0, 10);
        const escala = Math.max(1, ...maiores.flatMap(([, etapas, transportes]) =>
            [Number(etapas), Number(transportes)]));
        const grafico = document.createElement('section');
        grafico.className = 'grafico-produtividade';
        const titulo = document.createElement('h3');
        titulo.textContent = registros.length > 10
            ? 'Etapas e transportes distintos · 10 motoristas com mais etapas'
            : 'Etapas e transportes distintos';
        grafico.append(titulo);

        for (const [motorista, etapas, transportes] of maiores) {
            const grupo = document.createElement('div');
            grupo.className = 'grafico-produtividade-grupo';
            const nome = document.createElement('strong');
            nome.textContent = motorista;
            grupo.append(nome);

            for (const [rotulo, quantidade, classe] of [
                ['Etapas', etapas, 'etapas'],
                ['Transportes distintos', transportes, 'transportes']
            ]) {
                const serie = document.createElement('div');
                serie.className = 'grafico-produtividade-serie ' + classe;
                const texto = document.createElement('span');
                texto.textContent = rotulo + ' · ' + numero.format(quantidade);
                const trilho = document.createElement('span');
                trilho.className = 'grafico-produtividade-trilho';
                trilho.setAttribute('aria-hidden', 'true');
                const barra = document.createElement('span');
                barra.style.width = (Number(quantidade) / escala * 100) + '%';
                trilho.append(barra);
                serie.append(texto, trilho);
                grupo.append(serie);
            }
            grafico.append(grupo);
        }
        area.append(grafico);
    }

    for (const cartao of document.querySelectorAll('[data-transporte]')) {
        cartao.querySelector('[data-transporte-gerar]').addEventListener('click', () => abrirFiltros(cartao));
        cartao.querySelector('[data-transporte-ver]').addEventListener('click', () => abrirPrevia(cartao));
    }
    document.getElementById('transporte-imprimir').addEventListener('click', () => {
        const janela = window.open('', '_blank');
        const aviso = document.getElementById('transporte-aviso-impressao');
        aviso.textContent = '';
        if (!janela) {
            aviso.textContent = 'Permita a abertura de janelas para imprimir ou salvar em PDF.';
            return;
        }
        janela.opener = null;
        janela.document.title = previa.querySelector('h2').textContent;
        janela.document.documentElement.lang = 'pt-BR';
        const estilo = janela.document.createElement('style');
        estilo.textContent = '@page{size:A4 landscape;margin:14mm}body{font:14px Arial;color:#111}' +
            'table{width:100%;border-collapse:collapse}th,td{padding:6px;border:1px solid #aaa;text-align:left}' +
            'thead{display:table-header-group}tr{break-inside:avoid}';
        janela.document.head.append(estilo);
        janela.document.body.append(janela.document.importNode(document.getElementById('transporte-impressao'), true));
        const visualizacao = janela.document.getElementById('transporte-visualizacao');
        if (visualizacao.querySelector('.grafico-produtividade')) {
            janela.document.head.append(janela.document.importNode(
                document.getElementById('estilo-grafico-produtividade'), true));
        } else {
            visualizacao.remove();
        }
        janela.focus();
        janela.print();
    });
})();
