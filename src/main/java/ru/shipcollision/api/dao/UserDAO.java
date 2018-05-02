package ru.shipcollision.api.dao;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.shipcollision.api.controllers.MeController;
import ru.shipcollision.api.exceptions.InvalidCredentialsException;
import ru.shipcollision.api.exceptions.NotFoundException;
import ru.shipcollision.api.models.User;

import java.util.List;

@SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
@Service
@Transactional
public class UserDAO {

    public static final RowMapper<User> USER_ROW_MAPPER = (res, num) -> new User(
            res.getLong("id"),
            res.getString("username"),
            res.getString("email"),
            res.getInt("rank"),
            res.getString("avatar_link"),
            res.getString("password"));

    private final JdbcTemplate jdbcTemplate;

    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> getByRating(boolean ascending) {
        final String sqlQuery = "SELECT id, username, email, rank, avatar_link, password FROM users ORDER BY rank "
                + ((ascending) ? "ASC" : "DESC");

        return jdbcTemplate.query(sqlQuery, new Object[]{}, USER_ROW_MAPPER);
    }

    public User findById(Long id) {
        try {
            final String sqlQuery = "SELECT id, username, email, rank, avatar_link, password FROM users WHERE id = ?";
            return jdbcTemplate.queryForObject(sqlQuery, new Object[]{id}, USER_ROW_MAPPER);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(String.format("User with id %d not found", id));
        }
    }

    public User findByEmail(String email) {
        try {
            final String sqlQuery = "SELECT id, username, email, rank, avatar_link, password FROM users WHERE email = ?";
            return jdbcTemplate.queryForObject(sqlQuery, new Object[]{email}, USER_ROW_MAPPER);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(String.format("User with email %s not found", email));
        }
    }

    public User authenticate(String email, String password) {
        final User user = findByEmail(email);

        if (BCrypt.checkpw(password, user.password)) {
            return user;
        } else {
            throw new InvalidCredentialsException();
        }
    }

    public User save(User user) {
        final String sqlQuery;
        final Object[] queryParams;

        if (user.id != null) {
            sqlQuery = "INSERT INTO users(id, username, email, rank, avatar_link, password) "
                    + "values(?, ?, ?, ?, ?, ?) RETURNING id, username, email, rank, avatar_link, password";
            queryParams = new Object[]{
                    user.id,
                    user.username,
                    user.email,
                    user.rank,
                    user.avatarLink,
                    BCrypt.hashpw(user.password, BCrypt.gensalt())
            };
        } else {
            sqlQuery = "INSERT INTO users(username, email, rank, avatar_link, password) "
                    + "values(?, ?, ?, ?, ?) RETURNING id, username, email, rank, avatar_link, password";
            queryParams = new Object[]{
                    user.username,
                    user.email,
                    user.rank,
                    user.avatarLink,
                    BCrypt.hashpw(user.password, BCrypt.gensalt())
            };
        }

        try {
            return jdbcTemplate.queryForObject(sqlQuery, queryParams, USER_ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new InvalidCredentialsException(e);
        }
    }

    public User partialUpdate(Long id, MeController.PartialUpdateRequest updateRequest) {

        String encodedPassword = null;

        if (updateRequest.password != null) {
            encodedPassword = BCrypt.hashpw(updateRequest.password, BCrypt.gensalt());
        }

        final String sqlQuery = "UPDATE users SET "
                + "username = coalesce(?, username),"
                + "email = coalesce(?, email),"
                + "password = coalesce(?, password)"
                + "WHERE id = ? "
                + "RETURNING id, username, email, rank, avatar_link, password";
        return jdbcTemplate.queryForObject(
                sqlQuery,
                new Object[]{
                        updateRequest.username,
                        updateRequest.email,
                        encodedPassword,
                        id
                },
                USER_ROW_MAPPER);
    }
}
