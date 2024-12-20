package com.revature.library.Controller;

import com.revature.library.Exceptions.BookExceptions;
import com.revature.library.Exceptions.BookLogExceptions;
import com.revature.library.Exceptions.Unauthorized;
import com.revature.library.Exceptions.UserExceptions;
import com.revature.library.Models.Book;
import com.revature.library.Models.BookLog;
import com.revature.library.Models.User;
import com.revature.library.Service.BookLogService;
import com.revature.library.Service.BookService;
import com.revature.library.Service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class Controller {

    private final UserService userService;

    private final BookService bookService;

    private final BookLogService bookLogService;

    @Autowired
    public Controller(UserService userService, BookService bookService, BookLogService bookLogService) {
        this.userService = userService;
        this.bookService = bookService;
        this.bookLogService = bookLogService;
    }

    static String USERNAME_KEY = "username";

    void setUser(User user, HttpSession session){
        session.setAttribute(USERNAME_KEY, user.getUsername());
    }

    Optional<User> getUser(HttpSession session){
        if (session.getAttribute(USERNAME_KEY) == null){
            return Optional.empty();
        }

        return userService.getByUsername(
            (String)session.getAttribute(USERNAME_KEY)
        );
    }

    //region user
    @PostMapping("/users/login")
    public ResponseEntity<User> login(@RequestBody Map<String, String> body, HttpSession session) {
        try{
            var username = (String)body.get("username");
            var password = (String)body.get("password");

            var user = userService.login(username, password);

            setUser(user, session);

            return ResponseEntity.ok(user);
        }
        catch (Unauthorized  e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (NullPointerException|ClassCastException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/users/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(user));
        } catch (UserExceptions.NotAbsent | UserExceptions.UsernameInvalid | UserExceptions.EmailInvalid |
                 UserExceptions.PasswordInvalid e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/users/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        if (getUser(session).isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        session.invalidate();
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUser(HttpSession session) {
        try {
            return ResponseEntity.ok(
                userService.getAll(getUser(session))
            );
        } catch (Unauthorized  e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<User> getUser(@PathVariable String username, HttpSession session) {
        try {
            return ResponseEntity.ok(userService.getByUsername(username, getUser(session)));
        }
        catch (Unauthorized  e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (UserExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PatchMapping("/users/{username}")
    public ResponseEntity<User> updateUser(@PathVariable String username, @RequestBody User user, HttpSession session) {
        try {
            return ResponseEntity.ok(userService.editUser(username, user, getUser(session)));
        }
        catch (Unauthorized  e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (UserExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (UserExceptions.UsernameInvalid | UserExceptions.EmailInvalid | UserExceptions.PasswordInvalid e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username, HttpSession session) {
        try {
            userService.deleteUser(username, getUser(session));

            return ResponseEntity.ok().build();
        }
        catch (Unauthorized  e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (UserExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        catch (UserExceptions.IsHoldingBook e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    //endregion

    /*
     * get a single book
     * get all books
     * add books
     * edit book data
     * delete books(cannot delete books already issued)
     */
    //region books
    @GetMapping("/books/{bookId}")
    public ResponseEntity<Book> getBookById(@PathVariable int bookId) {
        try {
            return ResponseEntity.ok(bookService.getBookById(bookId));
        } catch (BookExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @PostMapping("/books")
    public ResponseEntity<Book> createNewBook(@RequestBody Book book, HttpSession session) {
        try {
            var newBook = bookService.createNewBook(book, getUser(session));

            return ResponseEntity.status(HttpStatus.CREATED).body(newBook);
        } 
        catch (Unauthorized  e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (BookExceptions.TitleAndAuthorAlreadyExists e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } 
    }

    @PatchMapping("/books/{bookId}")
    public ResponseEntity<Book> editBook(@PathVariable int bookId, @RequestBody Book book, HttpSession session) {
        try {
            return ResponseEntity.ok(bookService.editBook(bookId, book, getUser(session)));
        }
        catch (Unauthorized  e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (BookExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable int bookId, HttpSession session) {
        try {
            bookService.deleteBook(bookId, getUser(session));

            return ResponseEntity.ok().build();
        }
        catch (Unauthorized  e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (BookExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        catch (BookExceptions.IsHeld e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    //endregion

    /*
     * booklogs:
     * get books from a userid
     * get users from a bookid
     * get all logs
     */
    //region booklog
    @PostMapping("/bookLogs/{bookId}")
    ResponseEntity<BookLog> issueBook(@PathVariable int bookId, HttpSession session) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(bookLogService.issueBook(bookId, getUser(session)));
        }
        catch (Unauthorized e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (BookExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        catch (BookExceptions.IsHeld e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/bookLogs/return/{bookId}")
    public ResponseEntity<Void> returnBook(@PathVariable int bookId, HttpSession session) {
        try {
            bookLogService.returnBook(bookId, getUser(session));

            return ResponseEntity.ok().build();
        }
        catch (Unauthorized e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (BookLogExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        catch (BookExceptions.AlreadyReturned e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    //look into date to be returned
    @GetMapping("/bookLogs")
    public ResponseEntity<List<BookLog>> getAllLogs(HttpSession session) {
        try {
            return ResponseEntity.ok(bookLogService.getAll(getUser(session)));
        } catch (Unauthorized e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PatchMapping("/bookLogs/{logId}")
    public ResponseEntity<BookLog> editLog(@PathVariable int logId, @RequestBody BookLog bookLog, HttpSession session) {
        try {
            return ResponseEntity.ok(bookLogService.edit(logId, bookLog, getUser(session)));
        }
        catch (Unauthorized e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (BookLogExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/bookLogs/{logId}")
    public ResponseEntity<Void> deleteLog(@PathVariable int logId, HttpSession session) {
        try {
            bookLogService.delete(logId, getUser(session));

            return ResponseEntity.ok().build();
        }
        catch (Unauthorized e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (BookLogExceptions.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    //endregion
}