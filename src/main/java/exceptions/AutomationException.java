package exceptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Classe para ser utilizada para tratar quando ocorre uma falha na regra de negocio,
 * será finalizados os reports de evidencia com incluindo o erro informado.
 *
 * @author CONS388
 */
public class AutomationException extends RuntimeException {

    private static final long serialVersionUID = 8007642032621617075L;

    static final Logger logger = LogManager.getLogger(AutomationException.class);

    /**
     * Metodo apra criar uma exceção com o mensagem de erro formatada.
     * ex ("error messagem %s, %s", arg1, arg2)
     *
     * @param errorMsg mensagem de erro para ser exibida na exceção
     * @param args     argumentos para formatar a mensagem de erro da exceção
     */
    public AutomationException(String errorMsg, Object... args) {
        this(String.format(errorMsg, args));
    }

    /**
     * Metodo apra criar uma exceção com o mensagem de erro.
     *
     * @param errorMsg mensagem de erro para ser exibida na exceção
     */
    public AutomationException(String errorMsg) {
        super(errorMsg);
        logger.fatal(errorMsg);

    }

    /**
     * Metodo para criar uma exceção utilizando um objeto Exception
     *
     * @param exception Exceção a ser lançada
     */
    public AutomationException(Exception exception) {
        this(exception.getMessage());
    }

}
