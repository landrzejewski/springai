package pl.training.springai.model;

public record Book(
        String title,
        String authors,
        String description,
        int publicationYear
) {
}