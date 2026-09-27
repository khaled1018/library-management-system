# Library Management System — JPA Day 2

A small JPA + Hibernate + H2 project built to practice entity relationships,
ownership, fetching, cascading, inheritance, JPQL/HQL, and the Criteria API.

## Project layout

```
src/main/java/
├── Main.java                 (default package — runs every demo, in order)
├── dao/   AuthorDao, BookDao, PublisherDao, CategoryDao
└── entity/ Author, Book, Publisher, Category, Person, Employee, Customer

src/main/resources/META-INF/persistence.xml   (persistence unit: libraryPU)
```

## Build & run

```bash
mvn clean compile
mvn exec:java
```

(`exec:java` is preconfigured with `mainClass=Main` in `pom.xml`.) Everything
runs in one `main()` call against an H2 in-memory database, so no external
setup is required. `hibernate.show_sql=true` is on, so the console shows the
SQL behind every requirement below.

## 1. Relationship mappings, owning sides, FK locations

| Relationship | Type | Owning side | FK / join table |
|---|---|---|---|
| `Author` ↔ `Book` | `@OneToMany` / `@ManyToOne` | **`Book`** (`@JoinColumn(name="author_id")`) | `book.author_id` |
| `Book` → `Publisher` | `@ManyToOne` | **`Book`** | `book.publisher_id` |
| `Book` ↔ `Category` | `@ManyToMany` | **`Book`** (`@JoinTable(name="book_category")`) | join table `book_category(book_id, category_id)` |
| `Employee` / `Customer` → `Person` | inheritance | n/a | single table `person`, discriminator `person_type` |

- The owning side is the one that declares `@JoinColumn` / `@JoinTable`; only
  it writes the foreign key.
- `mappedBy = "author"` on `Author.books` makes `Author` the **inverse**
  side — it only reads the FK that `Book` owns.
- `@ManyToMany` needs a join table because neither `Book` nor `Category` can
  hold the other's FK alone.

## 2. Fetch choices

| Mapping | Fetch | Why |
|---|---|---|
| `Author.books` | `LAZY` | Could be a large collection; load only on demand |
| `Publisher.books` | `LAZY` (default for `@OneToMany`) | Same reasoning |
| `Category.books` | `LAZY` | Same reasoning |
| `Book.author` | `LAZY` (explicit) | `@ManyToOne` defaults to `EAGER`; overridden so listing books doesn't always pull in Author |
| `Book.publisher` | `LAZY` (explicit) | Same as above |

## 3. Cascade / orphanRemoval

- `Author.books` → `cascade = CascadeType.ALL`, `orphanRemoval = true`.
  - Persisting an `Author` cascades `PERSIST` to every `Book` in `books`
    (see `Main`: only `authorDao.save(...)` is called, never `bookDao.save`
    on those books directly).
  - Removing a `Book` from `author.getBooks()` deletes its row — demonstrated
    in `Main`'s "orphanRemoval demo" section.
- `Publisher.books` and `Category.books` have **no cascade** — publishers
  and categories are persisted explicitly through their own DAOs, since they
  have their own lifecycle independent of any one book.

## 4. Inheritance strategy — why `SINGLE_TABLE`

Chosen: `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` on `Person`,
with a `person_type` discriminator column, subclassed by `Employee` and
`Customer`.

- `Person`, `Employee`, and `Customer` share most columns
  (`id`, `firstName`, `lastName`, `email`); `SINGLE_TABLE` stores them once,
  no joins needed.
- Polymorphic queries stay cheap: `SELECT p FROM Person p` (used in `Main`)
  runs as a single `SELECT` over one table.
- Trade-off: subclass-only columns (`employee_code`, `membership_number`, …)
  must be nullable, and the table gets wider as more subclasses are added.
- Alternatives considered: `JOINED` (cleaner normalization, but every
  polymorphic query needs joins) and `TABLE_PER_CLASS` (no shared table, but
  `Person`-typed queries need `UNION`). For a small, shallow hierarchy like
  this one, `SINGLE_TABLE` is the simplest correct choice.

## 5. LazyInitializationException — what it is and how JOIN FETCH avoids it

`Main` reproduces this intentionally:

1. `authorDao.findById(id)` returns an `Author` whose `books` field is a
   Hibernate proxy (because `Author.books` is `LAZY`).
2. `em.clear()` detaches everything, closing off the proxy's access to the
   session.
3. Calling `lazyAuthor.getBooks().size()` then throws
   `LazyInitializationException`, because the proxy needs an open session to
   run its `SELECT` and there isn't one anymore.

`authorDao.findByIdWithBooks(id)` fixes this with
`LEFT JOIN FETCH a.books`: Hibernate loads the `Author` and its `Book`s in
one SQL statement, so the collection is already populated before the
`EntityManager` is even closed.

## 6. JPQL vs HQL

- **JPQL** is the portable subset defined by the Jakarta Persistence spec —
  every JPA provider must support it.
- **HQL** is Hibernate's superset of JPQL, adding functions JPQL doesn't
  have.

`Main` runs the same "authors with more than one book" query both ways:

```java
// Standard JPQL — portable
"SELECT a FROM Author a JOIN a.books b GROUP BY a HAVING COUNT(b) > 1"

// Hibernate-only HQL — size() isn't part of the JPQL spec
"SELECT a FROM Author a WHERE size(a.books) > :n"
```

Rule of thumb: write JPQL by default; drop to HQL only when a
Hibernate-specific feature is worth the lock-in.

## 7. Query decisions (Task 2 requirements)

| Requirement | Method | Decision |
|---|---|---|
| Books by author name | `BookDao.findByAuthorName` | Plain JPQL, implicit join through `b.author.name` |
| Books by publisher | `BookDao.findByPublisher` | JPQL with a managed `Publisher` entity as the parameter |
| Book by id, positional param | `BookDao.findByIdPositional` | Demonstrates `?1` syntax explicitly (named params used everywhere else for readability) |
| Author + Books, no N+1 | `AuthorDao.findByIdWithBooks` | `LEFT JOIN FETCH`, `DISTINCT` to collapse the duplicated parent row the join produces |
| Author name + book count | `AuthorDao.countBooksPerAuthor` | `LEFT JOIN` + `COUNT` + `GROUP BY` (LEFT so authors with 0 books still show) |
| Books by title | `BookDao.findByTitleCriteria` | Criteria API, single fixed predicate |
| Optional title/author filters | `BookDao.searchBooks` | Criteria API; predicates are only added to the list when the argument is non-null, so the generated SQL's `WHERE` clause changes shape per call |

