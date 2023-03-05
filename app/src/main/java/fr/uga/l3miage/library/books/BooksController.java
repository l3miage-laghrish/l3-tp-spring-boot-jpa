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
import jakarta.persistence.EntityExistsException;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.time.YearMonth;
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

    //////////////////////////////////////////////////////////

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

    /////////////////////////////////////////////////////////////

    @PostMapping("/authors/{authorId}/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO newBook(@PathVariable Long authorId, @RequestBody BookDTO book) {

        Book newBook = booksMapper.dtoToEntity(book); // transformer le livre du DTO à entity

        try {
            if (newBook.getTitle() == null) {
                throw new Exception("Le titre n'est pas valide.");
            }

            else if (newBook.getLanguage() != Language.ENGLISH
                    && newBook.getLanguage() != Language.FRENCH) {
                throw new Exception("Le langage n'est pas valide.");
            }

            // vérification du numéro Isbn
            else if (String.valueOf(newBook.getIsbn()).length() < 10) {
                throw new Exception("Le numéro Isbn n'est pas valide.");
            }
            // vérifier si l'année est dans le bon format (4 chiffres)
            else if (String.valueOf(newBook.getYear()).length() != 4) {
                throw new Exception("L'année n'est pas valide.");
            }
            Author author = authorService.get(authorId); //
            newBook.addAuthor(author); // on ajoute l'auteur au book
            bookService.save(authorId, newBook);

            return booksMapper.entityToDTO(newBook);
        } catch (EntityNotFoundException e) { // Si l'auteur n'est pas trouvé erreur 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "the author was not found", e);
        } catch (Exception f) { // 400
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    /////////////////////////////////////////////////////////

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

    ////////////////////////////////////////////////////////////

    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable("id") Long id) {

        try {
            // Si l'auteur partage l'autorité sur un livre, renvoyer une erreur 400
            bookService.delete(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Le livre n'est pas trouvé");
        }
    }

    //////////////////////////////////////////////////////////////////

    @PutMapping("/books/{id}/authors")
    @ResponseStatus(HttpStatus.OK)
    public BookDTO addAuthor(@PathVariable("id") Long bookId, @RequestBody AuthorDTO author) {

        try {
            Author a = authorService.get(author.id());
            if (a == null) {
                Author a2 = authorMapper.dtoToEntity(author);
                a = authorService.save(a2);
            }
            Book livre = bookService.get(bookId);
            bookService.save(a.getId(), livre);
            return this.booksMapper.entityToDTO(livre);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
