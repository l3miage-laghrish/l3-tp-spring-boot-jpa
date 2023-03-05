package fr.uga.l3miage.library.authors;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.books.BookDTO;
import fr.uga.l3miage.library.books.BooksMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.DeleteAuthorException;
import fr.uga.l3miage.library.service.EntityNotFoundException;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class AuthorsController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;
    private final BookService bookService;
    private final BooksMapper booksMapper;

    @Autowired
    public AuthorsController(AuthorService authorService, AuthorMapper authorMapper, BookService bookService,
            BooksMapper booksMapper) {
        this.authorService = authorService;
        this.authorMapper = authorMapper;
        this.bookService = bookService;
        this.booksMapper = booksMapper;
    }

    @GetMapping("/authors")
    public Collection<AuthorDTO> authors(@RequestParam(value = "q", required = false) String query) {
        Collection<Author> authors;
        if (query == null) {
            authors = authorService.list();
        } else {
            authors = authorService.searchByName(query);
        }
        return authors.stream()
                .map(authorMapper::entityToDTO)
                .toList();
    }

    ///////////////////////////////////////////////////

    @GetMapping("/authors/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorDTO author(@PathVariable("id") Long id) {

        Author author = null;
        try {
            // Récupération de l'ID
            author = authorService.get(id);

        } catch (EntityNotFoundException e) {
            // Dans le cas de l'ID incorrect
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "l'ID non trouvé");
        }

        if (author == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // conversion en DTO
        return authorMapper.entityToDTO(author);
    }

    ////////////////////////////////////////////////////

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDTO newAuthor(@RequestBody String authorString) {

        try {
            // Convertir la chaîne JSON en objet AUthorDTO
            AuthorDTO authorDTO = new ObjectMapper().readValue(authorString, AuthorDTO.class);

            // Vérifier que la chaîne fullName est non vide
            if (authorDTO.fullName() == null || authorDTO.fullName().trim().isEmpty()) {
                throw new IllegalArgumentException("l'auteur ne peut pas être null");
            }

            // Convertir authorDTO en entité Author
            Author author = authorMapper.dtoToEntity(authorDTO);

            // Sauvegarder l'entité Author
            author = this.authorService.save(author);

            // Convertir Author en DTO
            return authorMapper.entityToDTO(author);

        } catch (Exception e) {
            // Si une autre exception est levée lors de la conversion, retourner une erreur
            // 400
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les données fournies ne sont pas au bon format");
        }
    }

    //////////////////////////////////////////////////////

    /*
     * @PutMapping("/authors/{id}")
     * 
     * @ResponseStatus(HttpStatus.OK)
     * public AuthorDTO updateAuthor(@RequestBody AuthorDTO
     * author, @PathVariable("id") Long id) {
     * // attention AuthorDTO.id() doit être égale à id, sinon la requête
     * utilisateur
     * // est mauvaise
     * Author updAuthor;
     * try {
     * updAuthor = authorService.update(authorMapper.dtoToEntity(author));
     * return authorMapper.entityToDTO(updAuthor);
     * 
     * } catch (EntityNotFoundException e) {
     * throw new ResponseStatusException(HttpStatus.NOT_FOUND);
     * } catch (IllegalArgumentException e) {
     * throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
     * }
     * }
     */

    @PutMapping("/authors/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorDTO updateAuthor(@RequestBody AuthorDTO author, @PathVariable("id") Long id) {
        // attention AuthorDTO.id() doit être égale à id, sinon la requête utilisateur
        // est mauvaise
        Author a = authorMapper.dtoToEntity(author);
        Author aBd = null;
        if (a.getId() == id) {
            try {
                aBd = authorService.get(id);
                aBd.setFullName((a.getFullName()));
                authorService.update(aBd);

            } catch (EntityNotFoundException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return authorMapper.entityToDTO(aBd);
    }

    ////////////////////////////////////////////////////////////

    @DeleteMapping("/authors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable("id") Long id) {

        try {
            Author author = authorService.get(id);
            Book book = bookService.get(id);
            // Si l'auteur n'a pas de livres

            if (author.getBooks().isEmpty()) {
                authorService.delete(id);
            } else {
                if (book.getAuthors().size() > 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Les livres de cet auteur doivent être supprimer avant");
                } else {
                    bookService.delete(id);
                    authorService.delete(id);
                }
            }
            // Si l'auteur partage l'autorité sur un livre, renvoyer une erreur 400
        } catch (DeleteAuthorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les livres de cet auteur doivent être supprimer avant");
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'auteur n'est pas trouvé");
        }
    }

    @GetMapping("/authors/{id}/books")
    @ResponseStatus(HttpStatus.OK)
    public Collection<BookDTO> books(@RequestParam(value = "q", required = false) String name,
            @PathVariable("id") Long authorId) {

        // Récupérer l'auteur correspondant à l'ID
        Collection<Book> books;
        // Récupérer les livres pour cet auteur
        try {
            if (name != null && !name.isEmpty()) {
                // Filtrer par nom de livre si un nom est fourni
                books = bookService.findByAuthor(authorId, name);
            } else {
                books = bookService.getByAuthor(authorId);
            }

            // Convertir les livres en DTO et les retourner
            return books.stream()
                    .map(booksMapper::entityToDTO)
                    .toList();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        }
    }
}
