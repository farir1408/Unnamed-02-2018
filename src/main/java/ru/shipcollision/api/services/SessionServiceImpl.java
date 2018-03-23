package ru.shipcollision.api.services;

import org.springframework.stereotype.Service;
import ru.shipcollision.api.exceptions.ForbiddenException;
import ru.shipcollision.api.exceptions.NotFoundException;
import ru.shipcollision.api.models.User;

import javax.servlet.http.HttpSession;

/**
 * Сервис для работы с сессиями.
 */
@Service
public class SessionServiceImpl implements SessionService {

    /**
     * Имя куки, в которую будет записан идентификатор сессии.
     */
    public static final String ATTRIBUTE_NAME = "JSESSIONID";

    private final UserService userService;

    public SessionServiceImpl(UserService userService) {
        this.userService = userService;
    }

    /**
     * Метод для проверки, что хотя бы один пользователь авторизован.
     *
     * @return Проверяет, установлена ли кука.
     */
    private User getUserFromSession(HttpSession session) {
        final Object userId = session.getAttribute(ATTRIBUTE_NAME);

        if (userId == null)
            throw new ForbiddenException();

        try {
            return userService.findById((Long) userId);
        } catch (NotFoundException e) {
            session.removeAttribute(ATTRIBUTE_NAME);
            throw new ForbiddenException();
        }
    }

    /**
     * Записывает в куки идентификатор пользователя.
     *
     * @param user Пользователь, для которого будет открыта сессия.
     */
    @Override
    public void openSession(HttpSession session, User user) {
        session.setAttribute(ATTRIBUTE_NAME, user.id);
    }

    /**
     * Возвращает текущего пользователя.
     *
     * @return Пользователь открытой сессии.
     */
    @Override
    public User getCurrentUser(HttpSession session) {
        return getUserFromSession(session);
    }

    /**
     * Закрывает сессию для текущего пользователя.
     */
    @Override
    public void closeSession(HttpSession session) {
        if (getUserFromSession(session) != null) {
            session.removeAttribute(ATTRIBUTE_NAME);
        }
    }
}
