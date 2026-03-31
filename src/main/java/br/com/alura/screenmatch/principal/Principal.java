package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();

    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository serieRepository;

    public Principal(SerieRepository rep) {
        this.serieRepository = rep;
    }

    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBusca;

    public void exibeMenu() {

        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar Séries buscadas
                    4 - Buscar série por título
                    5 - Buscar série por ator
                    6 - Top séries
                    7 - Buscar por categoria
                    8 - Filtragem monstra
                    9 - Buscar episódio por trecho
                    10 - Buscar top Episódios por Série
                    11 - Buscar Episódios a partir de uma data
                    0 - Sair""";

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeries();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTopSeries();
                    break;
                case 7:
                    buscarPorCategoria();
                    break;
                case 8:
                    FiltrarSeriesPorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosPorData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        // dadosSeries.add(dados);
        Serie serie = new Serie(dados);
        serieRepository.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeries();
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = serieRepository.findByTituloContainingIgnoreCase(nomeSerie);
        List<DadosTemporada> temporadas = new ArrayList<>();

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            serieRepository.save(serieEncontrada);
        }
        else {
            System.out.println("Série não encontrada");
        }
    }

    private void listarSeries() {
        series = serieRepository.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = serieRepository.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()){
            System.out.println("Dados da série");
            System.out.println(serieBusca.get());
        }
        else {
            System.out.println("Série não encontrada");
        }

    }

    private void buscarSeriePorAtor() {
        System.out.println("Digite o nome do Ator/Atriz que deseja pesquisar");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliações a partir de qual valor?");
        var nota = leitura.nextDouble();

        List<Serie> seriesEncontradas = serieRepository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, nota);

        System.out.println("O artista " + nomeAtor + " atuou nas séries:");
        seriesEncontradas.forEach(s -> System.out.println(s.getTitulo() + " - Nota: " + s.getAvaliacao()));
    }

    private void buscarTopSeries() {
        List<Serie> seriesTop = serieRepository.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach(s -> System.out.println(s.getTitulo() + " - Nota: " + s.getAvaliacao()));
    }

    private void buscarPorCategoria() {
        System.out.println("Digite a categoria que deseja buscar");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesBuscadas = serieRepository.findByGenero(categoria);

        System.out.println("Séries cadastradas de " + categoria);
        seriesBuscadas.forEach(s -> System.out.println(s.getTitulo() + " - Nota: " + s.getAvaliacao()));
    }

    private void FiltrarSeriesPorTemporadaEAvaliacao(){
        System.out.println("Deseja filtrar por quantas temporadas?");
        var quantidadeTemporadas = leitura.nextInt();
        leitura.nextLine();

        System.out.println("Deseja filtrar a partir de qual avaliação?");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();

        List<Serie> filtroSeries = serieRepository.seriesPorTemporadaEAvaliacao(quantidadeTemporadas, avaliacao);
        System.out.println("***Séries filtradas***");
        filtroSeries.forEach(s -> System.out.println(s.getTitulo() + " - Avaliação: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Qual o nome do episódio para a busca?");
        var trechoEpisodio = leitura.nextLine();

        List<Episodio> episodiosAchados = serieRepository.episodiosPorTrecho(trechoEpisodio);

        System.out.println("***Episódios encontrados***");
        episodiosAchados.forEach(e -> System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                e.getSerie().getTitulo(), e.getTemporada(),
                e.getNumeroEpisodio(), e.getTitulo()));
    }

    private void topEpisodiosPorSerie() {
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = serieRepository.topEpisodiosPorSerie(serie);

            System.out.println("TOP Episódios");
            topEpisodios.forEach(e -> System.out.printf("Série: %s Temporada %s - Episódio %s - %s - Nota: %.2f\n",
                    e.getSerie().getTitulo(), e.getTemporada(),
                    e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));
        }
    }

    private void buscarEpisodiosPorData() {
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Digite o ano limite de lançamento");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = serieRepository.episodioPorSerieEAno(serie ,anoLancamento);

            System.out.println("Episódios achados");
            episodiosAno.forEach(System.out::println);
        }
    }
}