package fr.uga.l3miage.library.authors;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.books.BookDTO;
import jakarta.validation.Valid;

import org.mapstruct.Mapper;

import java.util.Collection;

@Mapper(componentModel = "spring")
public interface AuthorMapper {
    AuthorDTO entityToDTO(Author aBd);

    Collection<AuthorDTO> entityToDTO(Iterable<Author> authors);

    Author dtoToEntity(@Valid AuthorDTO authorDTO);

    Collection<Author> dtoToEntity(Iterable<AuthorDTO> authors);
}