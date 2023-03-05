package fr.uga.l3miage.library.books;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.data.domain.Book.Language;
import fr.uga.l3miage.library.authors.AuthorDTO;
import fr.uga.l3miage.library.authors.AuthorMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.PublicKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Flow.Publisher;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class BooksController {

    private final BookService bookService;
    private final BooksMapper booksMapper;
    private final AuthorService authorService;
    private final AuthorMapper authorMapper;

    @Autowired
    public BooksController(BookService bookService, BooksMapper booksMapper, AuthorService authorService,
            AuthorMapper authorMapper) {
        this.bookService = bookService;
        this.booksMapper = booksMapper;
        this.authorService = authorService;
        this.authorMapper = authorMapper;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/books")
    public Collection<BookDTO> books(String query) {

        Collection<Book> books;
        if (query == null) {
            books = bookService.list();
        } else {
            books = bookService.findByTitle(query);
        }
        return books.stream()
                .map(booksMapper::entityToDTO)
                .toList();
    }

    @GetMapping("/books/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookDTO book(@PathVariable("id") Long id) {

        Book book = null;

        try {
            book = bookService.get(id);

        } catch (EntityNotFoundException e) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (book == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return booksMapper.entityToDTO(book);
    }

    /*
     * @PostMapping("/authors/{id}/books")
     * 
     * @ResponseStatus(HttpStatus.CREATED)
     * public BookDTO newBook(@PathVariable("id") Long authorId, @Valid @RequestBody
     * BookDTO book) {
     * 
     * Book newBook = new Book();
     * newBook.setId(authorId);
     * 
     * try {
     * Author author = authorService.get(authorId);
     * 
     * // Vérifier que le titre n'est pas null ou vide
     * if (newBook.getTitle() == null || newBook.getTitle().isEmpty()) {
     * throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
     * "Le titre ne peut pas être null ou vide.");
     * }
     * newBook.setTitle(book.title());
     * 
     * // Vérifier que l'ISBN contient exactement 13 chiffres
     * String isbn = Long.toString(book.isbn());
     * if (isbn.length() != 13) {
     * throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
     * "L'ISBN doit contenir exactement 13 chiffres.");
     * }
     * newBook.setIsbn(book.isbn());
     * 
     * newBook.setPublisher(book.publisher());
     * 
     * // Vérifier que l'année est valide
     * String year = Short.toString(book.year());
     * if (year != null && !year.matches("\\d{4}")) {
     * throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année invalide.");
     * }
     * newBook.setYear(book.year());
     * 
     * // Validation du champ language
     * if (book.language() != null) {
     * String language = book.language().toLowerCase();
     * if (!language.equals("english") && !language.equals("french")) {
     * throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
     * "Langage invalide.");
     * }
     * if (language.equals("english")) {
     * newBook.setLanguage(Language.ENGLISH);
     * } else {
     * newBook.setLanguage(Language.FRENCH);
     * }
     * }
     * 
     * newBook.addAuthor(author);
     * bookService.save(authorId, newBook);
     * 
     * } catch (EntityNotFoundException e) {
     * throw new ResponseStatusException(HttpStatus.NOT_FOUND);
     * }
     * // Convertir le livre en DTO et le retourner
     * return booksMapper.entityToDTO(newBook);
     * }
     */

    @PostMapping("/authors/{id}/books")

    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO newBook(@PathVariable("id") Long authorId, @Valid @RequestBody BookDTO book) {

        try {

            Book newBook = booksMapper.dtoToEntity(book);
            // Collection<Author> newAuthor = authorMapper.dtoToEntity(book.authors());
            Author author = authorService.get(authorId);
            if (author == null) {
                authorService.save(author);
            }
            // Vérifier que le titre n'est pas null ou vide
            if (book.title() == null || book.title().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le titre ne peut pas être null ou vide.");
            }
            newBook.setTitle(book.title());

            // Vérifier que l'ISBN contient exactement 13 chiffres
            String isbn = Long.toString(book.isbn());
            if (isbn.length() != 13) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "L'ISBN doit contenir exactement 13 chiffres.");
            }
            newBook.setIsbn(book.isbn());

            newBook.setPublisher(book.publisher());

            // Vérifier que l'année est valide
            String year = Short.toString(book.year());
            if (year != null && !year.matches("\\d{4}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année invalide.");
            }
            newBook.setYear(book.year());

            // Validation du champ language
            if (book.language() != null) {
                String language = book.language().toLowerCase();
                if (!language.equals("english") && !language.equals("french")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Langage invalide.");
                }
                if (language.equals("english")) {
                    newBook.setLanguage(Language.ENGLISH);
                } else {
                    newBook.setLanguage(Language.FRENCH);
                }
            } else {
                newBook.setLanguage(Language.FRENCH);
            }

            newBook.addAuthor(author);
            bookService.save(authorId, newBook);

            // Convertir le livre en DTO et le retourner
            return booksMapper.entityToDTO(newBook);

        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auteur non trouvé");
        }
    }

    @PutMapping("/books/{id}")
    public BookDTO updateBook(@RequestBody BookDTO book, @PathVariable("id") Long id) {
        // attention BookDTO.id() doit être égale à id, sinon la requête utilisateur est
        // mauvaise
        // Vérifier que l'objet book n'est pas null
        Book b = booksMapper.dtoToEntity(book);
        Book bBd = null;
        if (b.getId() == id) {
            try {
                bBd = bookService.get(id);
                bBd.setTitle(b.getTitle());
                bBd.setIsbn(b.getIsbn());
                bBd.setPublisher(b.getPublisher());
                bBd.setYear(b.getYear());
                bBd.setLanguage(b.getLanguage());
                bookService.update(bBd);

            } catch (EntityNotFoundException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return booksMapper.entityToDTO(bBd);
    }

    public void deleteBook(Long id) {

        try {
            // Si l'auteur partage l'autorité sur un livre, renvoyer une erreur 400
            bookService.delete(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Le livre n'est pas trouvé");
        }
    }

    public void addAuthor(Long authorId, AuthorDTO author) {

    }
}
