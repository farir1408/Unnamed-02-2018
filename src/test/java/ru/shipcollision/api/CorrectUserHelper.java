package ru.shipcollision.api;

import com.github.javafaker.Faker;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import ru.shipcollision.api.exceptions.InvalidCredentialsException;
import ru.shipcollision.api.exceptions.NotFoundException;
import ru.shipcollision.api.models.User;
import ru.shipcollision.api.services.UserServiceImpl;

@SuppressWarnings("PublicField")
public class CorrectUserHelper {

    public static Long id;

    public static String username;

    public static String email;

    public static int rank;

    public static String password;

    static {
        final Faker faker = new Faker();

        id = (long) 1;
        username = faker.name().username();
        email = faker.internet().emailAddress();
        rank = 1;
        password = faker.internet().password();
    }

    public static User getCorrectUser() {
        final User correctUser = new User();

        correctUser.id = id;
        correctUser.username = username;
        correctUser.email = email;
        correctUser.rank = rank;
        correctUser.password = password;

        return correctUser;
    }

    public static void mockUserService(UserServiceImpl userService) {
        final User correctUser = CorrectUserHelper.getCorrectUser();

        // UserService будет отвечать успешно, только если кидать ему на вход
        // correctUser'a или его атрибуты. В остальных случаях будет исключение.
        Mockito.when(userService.hasEmail(correctUser.email))
                .thenReturn(true);
        Mockito.when(userService.hasEmail(AdditionalMatchers.not(Mockito.eq(correctUser.email))))
                .thenThrow(InvalidCredentialsException.class);

        Mockito.when(userService.hasId(correctUser.id))
                .thenReturn(true);
        Mockito.when(userService.hasId(AdditionalMatchers.not(Mockito.eq(correctUser.id))))
                .thenThrow(InvalidCredentialsException.class);

        Mockito.when(userService.hasUser(correctUser))
                .thenReturn(true);
        Mockito.when(userService.hasUser(AdditionalMatchers.not(Mockito.eq(correctUser))))
                .thenThrow(InvalidCredentialsException.class);

        Mockito.when(userService.hasusername(correctUser.username))
                .thenReturn(true);
        Mockito.when(userService.hasusername(AdditionalMatchers.not(Mockito.eq(correctUser.username))))
                .thenThrow(InvalidCredentialsException.class);

        Mockito.when(userService.findById(correctUser.id))
                .thenReturn(correctUser);
        Mockito.when(userService.findById(AdditionalMatchers.not(Mockito.eq(correctUser.id))))
                .thenThrow(NotFoundException.class);

        Mockito.when(userService.findByEmail(correctUser.email))
                .thenReturn(correctUser);
        Mockito.when(userService.findByEmail(AdditionalMatchers.not(Mockito.eq(correctUser.email))))
                .thenThrow(NotFoundException.class);
    }
}
