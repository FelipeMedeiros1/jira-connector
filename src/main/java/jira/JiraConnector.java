package jira;

import com.google.common.base.Strings;
import com.google.gson.Gson;


import data.InternalPropertiesLoader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JiraConnector {

    private static final Logger logger = LogManager.getLogger(JiraConnector.class);
    private boolean isActive;
    private String jiraBaseUrl;
    private String username;
    private String jiraKey;
    private static InternalPropertiesLoader pLoader = new InternalPropertiesLoader("configuration_core.properties");

    private static final JiraConnector instance = new JiraConnector();

    private JiraConnector() {
        initializeFromProperties();
    }

    private void initializeFromProperties() {
        try {
            this.isActive = Boolean.parseBoolean(pLoader.getValue("jira.connector.isActive"));

            if (isActive) {
                this.jiraBaseUrl = pLoader.getValue("jira.connector.baseUrl");
                this.username = pLoader.getValue("jira.connector.username");
                this.jiraKey = pLoader.getValue("jira.connector.jiraKey");

                if (Strings.isNullOrEmpty(this.jiraKey)) {
                    this.isActive = false;
                    logger.warn("Chave do projeto não configurada para conexão com o Jira.");
                } else {
                    logger.info("Conexão com o Jira foi ativada");
                }

                if (Strings.isNullOrEmpty(this.jiraBaseUrl) || Strings.isNullOrEmpty(this.username)
                        || Strings.isNullOrEmpty(this.jiraKey)) {
                    this.isActive = false;
                    logger.warn(
                            "Configurações incompletas para conexão com o Jira. Algumas propriedades estão ausentes ou vazias.");
                }
            } else {
                logger.warn("Conexão com o Jira não está ativa");
            }
        } catch (Exception e) {
            logger.error("Erro durante a inicialização do jira.JiraConnector", e);
            this.isActive = false;
        }
    }

    /**
     * Obtém a única instância de jira.JiraConnector.
     *
     * @return Instância de jira.JiraConnector.
     */
    public static JiraConnector getInstance() {
        return instance;
    }

    /**
     * Verifica se a conexão com o Jira está ativa.
     *
     * @return true se estiver ativa, false caso contrário.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Valida se o projeto no Jira é válido.
     *
     * @param projectKey Chave do projeto no Jira.
     * @return true se o projeto for válido, false caso contrário.
     */
    public boolean validateProject(String projectKey) {
        if (isActive()) {
            try {
                String jiraApiUrl = this.jiraBaseUrl + "/rest/api/2/project";
                validateUrl(jiraApiUrl);

                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpGet httpGet = new HttpGet(jiraApiUrl);
                setBasicAuthHeader(httpGet);

                HttpResponse response = httpClient.execute(httpGet);

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    logger.info("Projeto no Jira validado com sucesso: {}", projectKey);
                    return true;
                } else {
                    logger.error("Falha ao validar o projeto. Código de status: {}",
                            response.getStatusLine().getStatusCode());
                }
            } catch (IOException | URISyntaxException e) {
                logger.error("Erro ao validar o projeto no Jira", e);
            }
        }

        return false;
    }

    /**
     * Consulta as tarefas do projeto no Jira.
     *
     * @param projectKey Chave do projeto no Jira.
     */
    public void searchTasks(String projectKey) {
        if (isActive()) {
            try {
                String jiraApiUrl = this.jiraBaseUrl + "/rest/api/latest/search?jql=project=" + projectKey;
                validateUrl(jiraApiUrl);

                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpGet httpGet = new HttpGet(jiraApiUrl);
                setBasicAuthHeader(httpGet);

                HttpResponse response = httpClient.execute(httpGet);

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    logger.info("Consulta de tarefas do projeto {} realizada com sucesso.", projectKey);
                } else {
                    logger.error("Falha na consulta das tarefas do projeto. Código de status: "
                            + response.getStatusLine().getStatusCode());
                }
            } catch (IOException | URISyntaxException e) {
                logger.error("Erro durante a consulta das tarefas do projeto", e);
            }
        }
    }

    /**
     * Valida se a tarefa no Jira é válida.
     *
     * @param taskKey Chave da tarefa no Jira.
     * @return true se a tarefa for válida, false caso contrário.
     */
    public boolean validateTask(String taskKey) {
        if (isActive()) {
            try {
                String jiraApiUrl = this.jiraBaseUrl + "/rest/api/2/search";
                validateUrl(jiraApiUrl);

                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpGet httpGet = new HttpGet(jiraApiUrl);
                setBasicAuthHeader(httpGet);

                HttpResponse response = httpClient.execute(httpGet);

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    logger.info("Validação da tarefa {} realizada com sucesso.", taskKey);
                    return true;
                } else {
                    logger.error(
                            "Falha ao validar a tarefa. Código de status: " + response.getStatusLine().getStatusCode());
                }
            } catch (IOException | URISyntaxException e) {
                logger.error("Erro durante a validação da tarefa no Jira", e);
            }
        }

        return false;
    }

    /**
     * Atualiza os detalhes da tarefa no Jira.
     *
     * @param taskKey Chave da tarefa no Jira.
     * @param details Detalhes a serem atualizados.
     */
    public void updateTaskDetails(String taskKey, UpdateTaskDetails details) {
        if (isActive()) {
            try {
                String jiraApiUrl = this.jiraBaseUrl + "/rest/api/2/issue/" + taskKey;
                validateUrl(jiraApiUrl);

                HttpClient httpClient = HttpClients.createDefault();
                HttpPut httpPut = new HttpPut(jiraApiUrl);
                httpPut.setHeader(HttpHeaders.AUTHORIZATION, createBasicAuthHeader());
                httpPut.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("update", details.toMap());
                String jsonBody = new Gson().toJson(requestBody);
                httpPut.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

                HttpResponse response = httpClient.execute(httpPut);

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    logger.info("Detalhes da tarefa {} atualizados com sucesso.", taskKey);
                } else {
                    logger.error("Falha ao atualizar detalhes da tarefa. Código de status: "
                            + response.getStatusLine().getStatusCode());
                }

                EntityUtils.consume(response.getEntity());
            } catch (IOException | URISyntaxException e) {
                logger.error("Erro durante a atualização dos detalhes da tarefa no Jira", e);
            }
        }
    }

    /**
     * Transiciona uma tarefa para um novo status no Jira.
     *
     * @param taskKey  Chave da tarefa no Jira.
     * @param statusId ID do status para transição.
     */
    public void transitionIssue(String taskKey, String statusId) {
        if (isActive()) {
            try {
                // Construa a URL para realizar a transição de status
                String jiraApiUrl = this.jiraBaseUrl + "/rest/api/2/issue/" + taskKey + "/transitions";

                // Crie o cliente HTTP
                HttpClient httpClient = HttpClientBuilder.create().build();

                // Crie a solicitação POST para a transição
                HttpPost httpPost = new HttpPost(jiraApiUrl);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, createBasicAuthHeader());
                httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

                // Construa o corpo da solicitação com o ID da transição
                Map<String, Object> requestBody = Map.of("transition", Map.of("id", statusId));
                String jsonBody = new Gson().toJson(requestBody);
                httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

                // Execute a solicitação e obtenha a resposta
                HttpResponse response = httpClient.execute(httpPost);

                // Verifique se a resposta é bem-sucedida
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    logger.info("Transição de status da tarefa {} realizada com sucesso.", taskKey);
                } else {
                    logger.error("Falha na transição de status da tarefa. Código de status: "
                            + response.getStatusLine().getStatusCode());
                }

                // Certifique-se de consumir a entidade da resposta para liberar recursos
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
                logger.error("Erro durante a transição de status da tarefa no Jira", e);
            }
        }
    }

    /**
     * Adiciona um novo comentário a uma tarefa no Jira.
     *
     * @param taskKey Chave da tarefa no Jira.
     * @param comment Texto do comentário.
     */
    public void addComment(String taskKey, String comment) {
        if (isActive()) {
            try {
                // Construa a URL para adicionar um comentário
                String jiraApiUrl = this.jiraBaseUrl + "/rest/api/2/issue/" + taskKey + "/comment";

                // Crie o cliente HTTP
                HttpClient httpClient = HttpClientBuilder.create().build();

                // Crie a solicitação POST para adicionar um comentário
                HttpPost httpPost = new HttpPost(jiraApiUrl);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, createBasicAuthHeader());
                httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

                // Construa o corpo da solicitação com o texto do comentário
                Map<String, Object> requestBody = Map.of("body", comment);
                String jsonBody = new Gson().toJson(requestBody);
                httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

                // Execute a solicitação e obtenha a resposta
                HttpResponse response = httpClient.execute(httpPost);

                // Verifique se a resposta é bem-sucedida
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                    logger.info("Comentário adicionado com sucesso à tarefa {}.", taskKey);
                } else {
                    logger.error("Falha ao adicionar comentário. Código de status: "
                            + response.getStatusLine().getStatusCode());
                }

                // Certifique-se de consumir a entidade da resposta para liberar recursos
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
                logger.error("Erro ao adicionar comentário à tarefa no Jira", e);
            }
        }
    }

    /**
     * Adiciona uma evidência à tarefa no Jira.
     *
     * @param taskKey Chave da tarefa no Jira.
     */
    public void addEvidenceToTask(String taskKey) {
        if (isActive()) {
            try {
                String evidencePath;

                if(Strings.isNullOrEmpty(System.getenv("JENKINS_HOME")))
                    evidencePath = pLoader.getValue(System.getProperty("os.name").toUpperCase().contains("WINDOWS") ? "evidence.path.windows" : "evidence.path.unix").concat("/PDF/");
                else
                    evidencePath = pLoader.getValue("evidence.path.jenkins").concat("/PDF/");

                // Extrai apenas a parte após o "_" da tag
                String extractedTaskKey = taskKey.substring(taskKey.indexOf("_") + 1);

                // Obtém o arquivo PDF mais recente na pasta C:\Users\Public\Drivers\Reports\PDF
                File pdfFile = getLatestFile(evidencePath);
                if (pdfFile != null) {
                    // Construa a URL para adicionar uma evidência
                    String jiraApiUrl = String.format("%s/rest/api/3/issue/%s/attachments", this.jiraBaseUrl,
                            extractedTaskKey);

                    // Crie o cliente HTTP
                    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

                        // Crie a solicitação POST para adicionar uma evidência (PDF)
                        HttpPost httpPost = new HttpPost(jiraApiUrl);
                        httpPost.setHeader(HttpHeaders.AUTHORIZATION, createBasicAuthHeader());
                        httpPost.setHeader("X-Atlassian-Token", "no-check");
                        MultipartEntityBuilder pdfBuilder = MultipartEntityBuilder.create();
                        pdfBuilder.setBoundary("-------------" + System.currentTimeMillis());
                        pdfBuilder.addBinaryBody("file", pdfFile, ContentType.DEFAULT_BINARY, pdfFile.getName());
                        HttpEntity pdfEntity = pdfBuilder.build();
                        httpPost.setEntity(pdfEntity);

                        // Execute a solicitação e obtenha a resposta
                        try (CloseableHttpResponse pdfResponse = httpClient.execute(httpPost)) {
                            // Verifique se a resposta para o PDF é bem-sucedida
                            if (pdfResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                                logger.info("Evidência PDF adicionada com sucesso à tarefa {}.", extractedTaskKey);
                            }
                        }
                    }
                } else {
                    logger.error("Nenhum arquivo de evidência PDF encontrado na pasta.");
                }
            } catch (IOException e) {
                logger.error("Erro ao adicionar evidência à tarefa no Jira", e);
            }
        }
    }

    /**
     * Cria uma nova tarefa no Jira.
     *
     * @param projectKey  Chave do projeto no Jira.
     * @param summary     Resumo da tarefa.
     * @param description Descrição da tarefa.
     */
    public void createNewTask(String projectKey, String summary, String description) {
        if (isActive()) {
            try {
                String jiraApiUrl = this.jiraBaseUrl + "/rest/api/2/issue";
                validateUrl(jiraApiUrl);

                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost httpPost = new HttpPost(jiraApiUrl);
                setBasicAuthHeader(httpPost);
                httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

                // Construa o corpo da solicitação JSON
                Map<String, Object> requestBody = Map.of("fields", Map.of("project", Map.of("key", projectKey),
                        "summary", summary, "description", description, "issuetype", Map.of("name", "Tarefa")));

                String jsonBody = new Gson().toJson(requestBody);
                httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

                // Execute a solicitação e obtenha a resposta
                HttpResponse response = httpClient.execute(httpPost);

                // Verifique se a resposta é bem-sucedida
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    // Extrair chave do projeto
                    String createdIssueKey = new Gson().fromJson(responseBody, Map.class).get("key").toString();
                    logger.info("Nova tarefa criada com sucesso no projeto {}. Chave da tarefa: {}", projectKey,
                            createdIssueKey);

                } else {
                    logger.error("Falha ao criar nova tarefa. Código de status: "
                            + response.getStatusLine().getStatusCode());
                }

                // Certifique-se de consumir a entidade da resposta para liberar recursos
                EntityUtils.consume(response.getEntity());
            } catch (IOException | URISyntaxException e) {
                logger.error("Erro durante a criação da nova tarefa no Jira", e);
            }
        }
    }

    private void validateUrl(String url) throws URISyntaxException {
        new URI(url);
    }

    private void setBasicAuthHeader(HttpRequest request) {
        String credentials = this.username + ":" + this.jiraKey;
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + base64Credentials);
    }

    private String createBasicAuthHeader() {
        String credentials = this.username + ":" + this.jiraKey;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Detalhes a serem atualizados em uma tarefa no Jira.
     */
    public static class UpdateTaskDetails {

        private String summary;
        private String description;
        private List<String> labels;
        private String projectKey;

        /**
         * Converte os detalhes para um mapa.
         *
         * @return Mapa contendo os detalhes.
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (summary != null) {
                map.put("summary", Arrays.asList(Map.of("set", summary)));
            }
            if (description != null) {
                map.put("description", Arrays.asList(Map.of("set", description)));
            }
            if (labels != null) {
                map.put("labels", Arrays.asList(Map.of("set", labels)));
            }
            if (projectKey != null) {
                map.put("key", Arrays.asList(Map.of("set", projectKey)));
            }
            return map;
        }

        /**
         * Construtor dos detalhes da tarefa.
         *
         * @param summary     Resumo da tarefa.
         * @param description Descrição da tarefa.
         * @param labels      Etiquetas da tarefa.
         * @param statusId    Status de definição.
         * @param projectKey  Chave do projeto ao qual a tarefa está associada.
         */
        public UpdateTaskDetails(String summary, String description, List<String> labels, String statusId,
                                 String projectKey) {
            this.summary = summary;
            this.description = description;
            this.labels = labels;
            this.projectKey = projectKey;
        }
    }

    /**
     * Consulta uma tarefa no Jira e permite a atualização dos detalhes.
     *
     * @param projectKey Chave do projeto no Jira.
     * @param taskKey    Chave da tarefa no Jira.
     * @return Instância de TaskDetailsUpdater para atualização.
     */
    public static TaskDetailsUpdater consultTask(String projectKey, String taskKey) {

        JiraConnector jiraConnector = getInstance();

        if (jiraConnector.validateProject(projectKey)) {
            jiraConnector.searchTasks(projectKey);

            if (jiraConnector.validateTask(taskKey)) {
                return jiraConnector.new TaskDetailsUpdater(jiraConnector, taskKey);
            } else {
                logger.error("A tarefa {} não foi encontrada.", taskKey);
            }
        } else {
            logger.error("O projeto {} não é válido.", projectKey);
        }

        return null;
    }

    /**
     * Método para atualizar o resumo da tarefa.
     *
     * @param taskKey Chave da tarefa no Jira.
     * @param summary Novo resumo.
     */
    public static void updateSummary(String taskKey, String summary) {

        JiraConnector jiraConnector = getInstance();
        UpdateTaskDetails updateTaskDetails = new UpdateTaskDetails(summary, null, null, null, null);
        jiraConnector.updateTaskDetails(taskKey, updateTaskDetails);
    }

    /**
     * Método para atualizar a descrição da tarefa.
     *
     * @param taskKey     Chave da tarefa no Jira.
     * @param description Nova descrição.
     */
    public static void updateDescription(String taskKey, String description) {
        JiraConnector jiraConnector = getInstance();
        UpdateTaskDetails updateTaskDetails = new UpdateTaskDetails(null, description, null, null, null);
        jiraConnector.updateTaskDetails(taskKey, updateTaskDetails);
    }

    /**
     * Método para atualizar as etiquetas da tarefa.
     *
     * @param taskKey Chave da tarefa no Jira.
     * @param labels  Novas etiquetas.
     */
    public static void updateLabels(String taskKey, List<String> labels) {
        JiraConnector jiraConnector = getInstance();
        UpdateTaskDetails updateTaskDetails = new UpdateTaskDetails(null, null, labels, null, null);
        jiraConnector.updateTaskDetails(taskKey, updateTaskDetails);
    }

    /**
     * Método para atualizar o status da tarefa.
     *
     * @param taskKey  Chave da tarefa no Jira.
     * @param statusId ID do status a ser atualizado.
     */
    public static void updateStatus(String taskKey, String statusId) {
        JiraConnector jiraConnector = getInstance();
        jiraConnector.transitionIssue(taskKey, statusId);
    }

    /**
     * Método para adicionar um novo comentário a uma tarefa no Jira.
     *
     * @param taskKey Chave da tarefa no Jira.
     * @param comment Texto do comentário.
     */
    public static void addNewComment(String taskKey, String comment) {

        JiraConnector jiraConnector = getInstance();
        jiraConnector.addComment(taskKey, comment);
    }

    /**
     * Método para adicionar evidência a uma tarefa no Jira.
     *
     * @param taskKey Chave da tarefa no Jira.
     */
    public static void addEvidenceToATask(String taskKey) {
        JiraConnector jiraConnector = getInstance();
        jiraConnector.addEvidenceToTask(taskKey);
    }

    /**
     * Método para adicionar evidência a uma tarefa no Jira.
     *
     * @param taskKeys Lista de chaves das tarefas no Jira.
     */
    public static void addEvidence(Collection<String> taskKeys) {
        JiraConnector jiraConnector = getInstance();
        for (String taskKey : taskKeys) {
            if (taskKey.startsWith("@Jira_")) {
                jiraConnector.addEvidenceToTask(taskKey);
            }
        }
    }

    /**
     * Cria uma nova tarefa no Jira usando o método apply.
     *
     * @param projectKey  Chave do projeto no Jira.
     * @param summary     Resumo da tarefa.
     * @param description Descrição da tarefa.
     */
    public static void createTask(String projectKey, String summary, String description) {
        JiraConnector jiraConnector = getInstance();
        jiraConnector.createNewTask(projectKey, summary, description);
    }

    /**
     * Atualizador de detalhes de uma tarefa no Jira.
     */
    public class TaskDetailsUpdater {

        private final JiraConnector jiraConnector;
        private String projectKey;
        private String taskKey;
        private String summary;
        private String description;
        private List<String> labels;
        private String statusId;
        private String comment;

        /**
         * Adiciona um novo comentário à tarefa.
         *
         * @param comment Texto do comentário.
         * @return Instância do TaskDetailsUpdater.
         */
        public TaskDetailsUpdater addComment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Construtor do atualizador de detalhes da tarefa.
         *
         * @param jiraConnector Instância do jira.JiraConnector.
         * @param taskKey       Chave da tarefa no Jira.
         */
        public TaskDetailsUpdater(JiraConnector jiraConnector, String taskKey) {
            this.jiraConnector = jiraConnector;
            this.taskKey = taskKey;
        }

        /**
         * Atualiza o resumo da tarefa.
         *
         * @param summary Novo resumo.
         * @return Instância do TaskDetailsUpdater.
         */
        public TaskDetailsUpdater updateSummary(String summary) {
            this.summary = summary;
            return this;
        }

        /**
         * Atualiza a descrição da tarefa.
         *
         * @param description Nova descrição.
         * @return Instância do TaskDetailsUpdater.
         */
        public TaskDetailsUpdater updateDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Atualiza as etiquetas da tarefa.
         *
         * @param labels Novas etiquetas.
         * @return Instância do TaskDetailsUpdater.
         */
        public TaskDetailsUpdater updateLabels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        /**
         * Atualiza o status da tarefa.
         *
         * @param statusId ID do status a ser atualizado.
         * @return Instância do TaskDetailsUpdater.
         */
        public TaskDetailsUpdater updateStatus(String statusId) {
            this.statusId = statusId;
            return this;
        }

        /**
         * Adiciona uma evidência à tarefa.
         *
         * @param taskKey Chave da tarefa no Jira.
         * @return Instância do TaskDetailsUpdater.
         */
        public TaskDetailsUpdater addEvidence(String taskKey) {
            jiraConnector.addEvidenceToTask(taskKey);
            this.taskKey = taskKey;
            return this;
        }

        /**
         * Adiciona uma evidência à tarefa.
         *
         * @return Instância do TaskDetailsUpdater.
         */
        public TaskDetailsUpdater createTask(String projectKey, String summary, String description) {
            jiraConnector.createNewTask(projectKey, summary, description);
            this.projectKey = projectKey;
            this.summary = summary;
            this.description = description;
            return this;
        }

        /**
         * Aplica as atualizações à tarefa no Jira.
         */
        public void apply() {
            UpdateTaskDetails updateTaskDetails = new UpdateTaskDetails(summary, description, labels, statusId,
                    projectKey);

            if (comment != null) {
                jiraConnector.addComment(taskKey, comment);
            }

            jiraConnector.updateTaskDetails(taskKey, updateTaskDetails);
            jiraConnector.transitionIssue(taskKey, statusId);
            jiraConnector.addEvidenceToTask(taskKey);
            jiraConnector.createNewTask(projectKey, summary, description);
        }
    }

    /**
     * Encapsula a funcionalidade de adicionar um novo comentário a uma tarefa no
     * Jira.
     */
    public class NewComment {

        private final JiraConnector jiraConnector;
        private String taskKey;
        private String comment;

        /**
         * Construtor da funcionalidade de adicionar um novo comentário.
         *
         * @param jiraConnector Instância do jira.JiraConnector.
         * @param taskKey       Chave da tarefa no Jira.
         */
        public NewComment(JiraConnector jiraConnector, String taskKey) {
            this.jiraConnector = jiraConnector;
            this.taskKey = taskKey;
        }

        /**
         * Define o texto do comentário.
         *
         * @param comment Texto do comentário.
         * @return Instância do NewComment.
         */
        public NewComment withComment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Aplica a adição do novo comentário à tarefa no Jira.
         */
        public void apply() {
            jiraConnector.addComment(taskKey, comment);
        }
    }

    /**
     * Obtém o arquivo mais recente em uma pasta.
     *
     * @param folderPath Caminho da pasta.
     * @return Arquivo mais recente ou null se a pasta estiver vazia.
     */
    private File getLatestFile(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        if (files != null && files.length > 0) {
            // Ordena os arquivos por data de modificação, do mais recente para o mais antigo
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            return files[0];
        } else {
            return null;
        }
    }
}
