package jira;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import data.InternalPropertiesLoader;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;


/**
 * jira.ZephyrConnector - Conecta e interage com o Zephyr.
 */
public class ZephyrConnector {

    private static final Logger logger = LogManager.getLogger(ZephyrConnector.class);
    private static boolean isActive;
    private static String zephyrUrl = "https://api.zephyrscale.smartbear.com";
    private static String projectId;
    private static String zephyrKey;
    private static InternalPropertiesLoader pLoader = new InternalPropertiesLoader("configuration_core.properties");
    private static final HttpClient httpClient = HttpClientBuilder.create().build();
    private static final HttpClient httpClient1 = HttpClientBuilder.create().build();

    private static final ZephyrConnector instance = new ZephyrConnector();

    private ZephyrConnector() {
        initializeFromProperties();
    }

    /**
     * Inicializa a instância do jira.ZephyrConnector a partir das propriedades.
     */
    private void initializeFromProperties() {
        try {
            isActive = Boolean.parseBoolean(pLoader.getValue("zephyr.connector.isActive"));

            if (isActive) {
                zephyrKey = pLoader.getValue("zephyr.connector.zephyrKey");
                projectId = pLoader.getValue("zephyr.connector.projectId");
                if (Strings.isNullOrEmpty(zephyrKey) || Strings.isNullOrEmpty(projectId)) {
                    isActive = false;
                    logger.warn("Configurações ausentes para conexão com o Zephyr. Verifique os valores.");
                }
            }
        } catch (Exception e) {
            logger.error("Erro durante a inicialização do jira.ZephyrConnector", e);
            isActive = false;
        }
    }

    /**
     * Obtém a única instância de jira.ZephyrConnector.
     *
     * @return Instância de jira.ZephyrConnector.
     */
    public static ZephyrConnector getInstance() {
        return instance;
    }

    /**
     * Verifica se a conexão com o Zephyr está ativa.
     *
     * @return true se estiver ativa, false caso contrário.
     */
    public static boolean isActive() {
        return isActive;
    }

    /**
     * Cria uma execução de teste no Zephyr usando os parâmetros fornecidos.
     *
     * @param tags           Lista de tags associadas à tarefa.
     * @param scenarioStatus Status do cenário (true se PASSADO, false se FALHADO).
     * @param executionTime  Tempo de execução.
     */
    public static void createExecutionTest(Collection<String> tags, boolean scenarioStatus, long executionTime) {
        String projectKey = projectId;
        String testCaseKey = getTestCaseKeyByTags(tags);
        String testCycleKey = getCycleCaseKeyByTags(tags);
        String statusName = scenarioStatus ? "Pass" : "Fail";
        String zephyrApiUrl = zephyrUrl + "/v2/testexecutions";
        if (isActive()) {
            try {
                validateUrl(zephyrApiUrl);

                // Cria uma requisição HTTP POST
                HttpPost httpPost = new HttpPost(new URI(zephyrApiUrl));
                setHeaders(httpPost);

                // Constrói o corpo da requisição JSON
                String requestBody = String.format(
                        "{ \"projectKey\": \"%s\", \"testCaseKey\": \"%s\", \"testCycleKey\": \"%s\", "
                                + "\"statusName\": \"%s\", \"executionTime\": %d }",
                        projectKey, testCaseKey, testCycleKey, statusName, executionTime);

                // Adiciona o corpo à requisição POST
                httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

                // Executa a requisição POST
                HttpResponse response = httpClient1.execute(httpPost);

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                    logger.info("Criação da execução de teste realizada com sucesso.");
                    // Lógica adicional conforme necessário
                }
            } catch (IOException | URISyntaxException e) {
                logger.error("Erro durante a criação da execução de teste", e);
            }
        }
    }

    /**
     * Atualiza o status do ciclo de teste.
     *
     * @param tags      Lista de tags associadas à tarefa.
     * @param newStatus Novo status a ser definido.
     */
    public static void updateCycleStatus(Collection<String> tags, String newStatus) {
        String projectKey = projectId;
        String testCaseKey = getTestCaseKeyByTags(tags);
        String testCycleKey = getCycleCaseKeyByTags(tags);
        String zephyrApiUrl = zephyrUrl + "/v2/testexecutions";

        try {
            // Constrói a URL da API do Zephyr
            validateUrl(zephyrApiUrl);

            // Cria uma requisição HTTP POST
            HttpPost httpPost = new HttpPost(new URI(zephyrApiUrl));
            setHeaders(httpPost);

            // Constrói o corpo da requisição JSON
            String requestBody = String.format(
                    "{ \"projectKey\": \"%s\", \"testCaseKey\": \"%s\", \"testCycleKey\": \"%s\", "
                            + "\"statusName\": \"%s\", \"executionTime\": 0 }",
                    projectKey, testCaseKey, testCycleKey, newStatus);

            // Adiciona o corpo à requisição POST
            httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

            // Executa a requisição POST
            HttpResponse response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                logger.info("Ciclo de teste atualizado para '{}' para o {}.", newStatus, testCycleKey);
                // Lógica adicional conforme necessário
            }
        } catch (IOException | URISyntaxException e) {
            logger.error("Erro durante a atualização do status do ciclo de teste", e);
        }
    }

    /**
     * Verifica se um ciclo de teste existe no Zephyr.
     *
     * @param testCycleKey Chave do ciclo de teste.
     * @return true se o ciclo existe, false caso contrário.
     */
    public static boolean testCycleExists(String testCycleKey) {
        try {
            // Constrói a URL da API do Zephyr
            String zephyrApiUrl = zephyrUrl + "/v2/testcycles/" + testCycleKey;
            validateUrl(zephyrApiUrl);

            // Cria uma requisição HTTP GET
            HttpGet httpGet = new HttpGet(zephyrApiUrl);
            setHeaders(httpGet);

            // Executa a requisição GET
            HttpResponse response = httpClient.execute(httpGet);

            // Retorna true se o código de status for OK (200)
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (Exception e) {
            logger.error("Erro ao verificar a existência do ciclo de teste", e);
            return false;
        }
    }

    /**
     * Verifica se os ciclos de teste existem no Zephyr.
     *
     * @param testCycleKeys Lista de chaves dos ciclos de teste.
     * @return true se todos os ciclos existem, false caso contrário.
     */
    public static boolean testCyclesExist(Collection<String> testCycleKeys) {
        for (String cycleKey : testCycleKeys) {
            if (!ZephyrConnector.testCycleExists(cycleKey)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica se os ciclos de teste existem no Zephyr.
     *
     * @param scenarioTags Lista de tags associadas ao cenário.
     * @return Chave do caso de teste ou null se não encontrada.
     */
    public static String getTestCaseKeyByTags(Collection<String> scenarioTags) {
        for (String tag : scenarioTags) {
            if (tag.startsWith("@Key_")) {
                return tag.replace("@Key_", "").trim();
            }
        }
        return null;
    }

    /**
     * Verifica se os ciclos de teste existem no Zephyr.
     *
     * @param scenarioTags Lista de tags associadas ao cenário.
     * @return Chave do caso de teste ou null se não encontrada.
     */
    public static String getCycleCaseKeyByTags(Collection<String> scenarioTags) {
        for (String tag : scenarioTags) {
            if (tag.startsWith("@Zephyr_")) {
                return tag.replace("@Zephyr_", "").trim();
            }
        }
        return null;
    }

    /**
     * Configura os cabeçalhos da requisição HTTP.
     *
     * @param httpRequest Requisição HTTP a ser configurada.
     */
    private static void setHeaders(HttpPost httpRequest) {
        httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + zephyrKey);
        httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
    }

    /**
     * Define os cabeçalhos da requisição HTTP com as informações de autorização e
     * tipo de conteúdo.
     *
     * @param httpRequest Objeto representando a requisição HTTP.
     */
    private static void setHeaders(HttpUriRequest httpRequest) {
        httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + zephyrKey);
        httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
    }

    private static void setAuthorizationHeaders(HttpUriRequest httpRequest) {
        httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + zephyrKey);
    }

    public void setAuthorizationAndContentTypeHeaders(HttpUriRequest httpRequest) {
        setAuthorizationHeaders(httpRequest);
        httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
    }

    /**
     * Valida uma URL.
     *
     * @param url URL a ser validada.
     * @throws URISyntaxException Se a URL for inválida.
     */
    private static void validateUrl(String url) throws URISyntaxException {
        new URI(url);
    }

}
