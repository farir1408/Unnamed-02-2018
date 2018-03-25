package ru.shipcollision.api.controllers;

import com.github.javafaker.Faker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.shipcollision.api.CorrectUserHelper;
import ru.shipcollision.api.exceptions.ForbiddenException;
import ru.shipcollision.api.models.User;
import ru.shipcollision.api.services.SessionServiceImpl;
import ru.shipcollision.api.services.UserServiceImpl;

import java.util.List;
import java.util.stream.Stream;

/**
 * Тест контроллера сессий пользователя.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Тест конктроллера сессий")
public class SessionsControllerTest {

    public static final String SIGNIN_ROUTE = "/signin";

    public static final String SIGNOUT_ROUTE = "/signout";

    @MockBean
    private SessionServiceImpl sessionService;

    @MockBean
    private UserServiceImpl userService;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private static Stream<Arguments> provideIncorrectCredentials() {
        final Faker faker = new Faker();

        return Stream.of(
                Arguments.of(CorrectUserHelper.email, faker.internet().password()),
                Arguments.of(faker.internet().emailAddress(), CorrectUserHelper.password),
                Arguments.of(faker.internet().emailAddress(), faker.internet().password())
        );
    }

    /**
     * Эмуляция залогиненного пользователя.
     */
    private void mockSessionServiceWithUser() {
        final User correctUser = CorrectUserHelper.getCorrectUser();
        Mockito.when(sessionService.getCurrentUser(Mockito.any())).thenReturn(correctUser);
    }

    /**
     * Эмуляция незалогиненного пользователя.
     */
    private void mockSessionServiceWithoutUser() {
        Mockito.when(sessionService.getCurrentUser(Mockito.any())).thenThrow(ForbiddenException.class);
    }

    @BeforeEach
    public void mockUserService() {
        CorrectUserHelper.mockUserService(userService);
    }

    @Test
    @DisplayName("можно войти из-под корректного аккаунта")
    public void testCorrectSignin() {
        mockSessionServiceWithUser();

        final User user = CorrectUserHelper.getCorrectUser();
        final SessionsController.SigninRequest signinRequest =
                new SessionsController.SigninRequest(user.email, user.password);
        final ResponseEntity<User> response = testRestTemplate.postForEntity(SIGNIN_ROUTE, signinRequest, User.class);

        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);

        final HttpHeaders responseHeaders = response.getHeaders();
        final User responseUser = response.getBody();

        Assertions.assertNotNull(responseHeaders);
        final List<String> cookies = responseHeaders.get("Set-Cookie");
        Assertions.assertNotNull(cookies);
        Assertions.assertFalse(cookies.isEmpty());

        Assertions.assertNotNull(responseUser);
        Assertions.assertEquals(responseUser.email, user.email);
        Assertions.assertEquals(responseUser.username, user.username);
        Assertions.assertNull(responseUser.password);
    }

    @ParameterizedTest
    @MethodSource("provideIncorrectCredentials")
    @DisplayName("нельзя войти, если логин и/или пароль не совпадает с корректным")
    public void testIncorrectSignin(String email, String password) {
        mockSessionServiceWithUser();

        final SessionsController.SigninRequest request =
                new SessionsController.SigninRequest(email, password);
        final ResponseEntity<Object> response = testRestTemplate.postForEntity(SIGNIN_ROUTE, request, Object.class);

        Assertions.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("можно выйти, если был осуществлен вход")
    public void testCorrectSignout() {
        mockSessionServiceWithUser();
        final ResponseEntity<Object> response =
                testRestTemplate.exchange(SIGNOUT_ROUTE, HttpMethod.DELETE, null, Object.class);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Test
    @DisplayName("403 при попытке выхода, если сессии нет в куках")
    public void testNoUserSignout() {
        mockSessionServiceWithoutUser();
        final ResponseEntity<Object> response =
                testRestTemplate.exchange(SIGNOUT_ROUTE, HttpMethod.DELETE, null, Object.class);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.FORBIDDEN);
    }
}
