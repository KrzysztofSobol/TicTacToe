# Zadanie 3 – Gra Kółko i Krzyżyk w technologii Java RMI

## Spis treści
1. [Opis projektu](#1-opis-projektu)
2. [Architektura](#2-architektura)
3. [Struktura plików](#3-struktura-plików)
4. [Opis klas i metod](#4-opis-klas-i-metod)
5. [Przepływ działania aplikacji](#5-przepływ-działania-aplikacji)
6. [Instrukcja uruchomienia](#6-instrukcja-uruchomienia)
7. [Jak wygląda rozgrywka](#7-jak-wygląda-rozgrywka)

---

## 1. Opis projektu

Implementacja gry **kółko i krzyżyk** (tic-tac-toe) z użyciem technologii **Java RMI** (Remote Method Invocation). Projekt składa się z dwóch oddzielnych modułów IntelliJ:

- **server** – obiekt zdalny `KKSerwer`, zawiera całą logikę gry
- **client** – obiekt gracza `KKlient`, wywołuje zdalne metody serwera

Gra jest **w pełni konsolowa**. Cała logika gry (walidacja ruchów, wykrywanie wygranej/remisu, zarządzanie stanem planszy) realizowana jest **wyłącznie po stronie serwera**. Klient odpowiada jedynie za interfejs użytkownika i komunikację z serwerem.

---

## 2. Architektura

```
┌──────────────────────────────────────────────────────────────┐
│                     KKlient (client)                         │
│  Main.java                                                   │
│  - łączy się z RMI Registry                                  │
│  - wywołuje zdalne metody TicTacToeService                   │
│  - wyświetla planszę w konsoli                               │
│  - wczytuje ruch gracza (wiersz, kolumna)                    │
│  - polluje stan gry co 1 sekundę gdy czeka na przeciwnika    │
└──────────────┬───────────────────────────────────────────────┘
               │  Java RMI (TCP, domyślnie port 1099)
               │  Serializowane obiekty: GameState, MoveResult
               ▼
┌──────────────────────────────────────────────────────────────┐
│                     KKSerwer (server)                        │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐     │
│  │ RMI Registry (port 1099)                            │     │
│  │ Nazwa: "TicTacToe"                                  │     │
│  └─────────────────────────────────────────────────────┘     │
│                                                              │
│  TicTacToeServiceImpl (UnicastRemoteObject)                  │
│  - rejestracja graczy (max 2)                                │
│  - plansza char[3][3]                                        │
│  - walidacja ruchów (synchronized)                           │
│  - wykrywanie wygranej / remisu                              │
│  - zarządzanie kolejnością ruchów                            │
└──────────────────────────────────────────────────────────────┘
```

### Kluczowe mechanizmy RMI

| Mechanizm | Opis |
|-----------|------|
| `UnicastRemoteObject` | Bazowa klasa serwera; eksportuje obiekt przez RMI |
| `Remote` interface | `TicTacToeService` – kontrakt metod zdalnych |
| `Serializable` | `GameState`, `MoveResult` – przesyłane przez sieć jako kopie |
| RMI Registry | Usługa katalogowa; serwer rejestruje obiekt, klient go wyszukuje |
| `Naming.rebind/lookup` | Rejestracja i wyszukiwanie obiektu zdalnego |

### Model komunikacji

Komunikacja jest **jednostronna** (klient → serwer). Klient nie jest obiektem zdalnym – serwer nie wywołuje metod klienta. Zamiast callbacków zastosowano **polling**: klient co sekundę pyta serwer o stan gry (`getGameState()`).

---

## 3. Struktura plików

```
zadanie_3/
├── README.md
│
├── server/                              # KKSerwer – moduł IntelliJ
│   ├── server.iml
│   ├── .idea/
│   └── src/
│       ├── Main.java                    # Punkt wejścia serwera
│       └── pl/edu/rsi/tictactoe/
│           ├── TicTacToeService.java    # Interfejs zdalny (Remote)
│           ├── TicTacToeServiceImpl.java # Logika gry (UnicastRemoteObject)
│           ├── GameState.java           # Stan gry (Serializable)
│           └── MoveResult.java          # Wynik ruchu (Serializable)
│
└── client/                              # KKlient – moduł IntelliJ
    ├── client.iml
    ├── .idea/
    └── src/
        ├── Main.java                    # Punkt wejścia klienta
        └── pl/edu/rsi/tictactoe/
            ├── TicTacToeService.java    # (kopia) interfejs zdalny
            ├── GameState.java           # (kopia) stan gry
            └── MoveResult.java          # (kopia) wynik ruchu
```

> **Uwaga:** Klasy `TicTacToeService`, `GameState` i `MoveResult` muszą być identyczne w obu modułach – zarówno kod, jak i pełna nazwa pakietu (`pl.edu.rsi.tictactoe`). Java RMI wymaga zgodności klas po obu stronach połączenia przy deserializacji.

---

## 4. Opis klas i metod

### `TicTacToeService` (interfejs zdalny)

Dziedziczy po `java.rmi.Remote`. Każda metoda deklaruje `throws RemoteException`.

| Metoda | Opis |
|--------|------|
| `String joinGame(String playerName)` | Dołącza do gry. Zwraca token `"playerId:symbol"` (np. `"abc123...:X"`). Rzuca `RemoteException` gdy gra jest pełna. |
| `MoveResult makeMove(String playerId, int row, int col)` | Wykonuje ruch. Waliduje po kolei: istnienie gracza, status gry, kolejność, zakres pozycji, zajętość pola. Sprawdza wygraną i remis. |
| `GameState getGameState(String playerId)` | Zwraca aktualny stan gry (plansza, status, czyja kolej, wynik). |
| `boolean isMyTurn(String playerId)` | Zwraca `true` gdy gra jest `IN_PROGRESS` i to kolej tego gracza. |
| `void resetGame()` | Resetuje planszę po zakończeniu gry. Gracze pozostają zarejestrowani; X zaczyna od nowa. |

---

### `TicTacToeServiceImpl` (logika gry)

Dziedziczy po `UnicastRemoteObject`, implementuje `TicTacToeService`.

**Stan wewnętrzny:**
- `char[][] board` – plansza 3×3, wartości: `' '` (puste), `'X'`, `'O'`
- `Map<String, Player> players` – zarejestrowani gracze (max 2)
- `String currentPlayerId` – ID gracza, który ma teraz ruch
- `GameState.Status status` – aktualny status gry
- `String winnerPlayerId` – ID zwycięzcy (lub `null`)

**Wewnętrzna klasa `Player`:**
```
Player { String id, String name, char symbol }
```

**Metody prywatne:**

| Metoda | Opis |
|--------|------|
| `clearBoard()` | Wypełnia planszę spacjami (puste pola). |
| `checkWin(char sym)` | Sprawdza 3 wiersze, 3 kolumny i 2 przekątne pod kątem 3 w rzędzie. |
| `checkDraw()` | Zwraca `true` gdy brak wolnych pól (i wcześniej nie stwierdzono wygranej). |
| `buildGameState(Player p)` | Tworzy obiekt `GameState` z aktualnego stanu. Tworzy defensywną kopię planszy (`clone()`). |
| `printServerBoard()` | Wyświetla planszę w konsoli serwera po każdym ruchu (monitoring). |

**Synchronizacja:** wszystkie metody publiczne oznaczone `synchronized` – bezpieczne dla wielu wątków (jeden wątek na klienta w modelu RMI).

---

### `GameState` (Serializable)

Obiekt przesyłany przez RMI jako **kopia** (value object). Zawiera stan gry w momencie wywołania.

| Pole | Typ | Opis |
|------|-----|------|
| `board` | `char[3][3]` | Stan planszy (`' '`, `'X'`, `'O'`) |
| `status` | `Status` | `WAITING_FOR_PLAYER`, `IN_PROGRESS`, `WIN`, `DRAW` |
| `currentPlayerSymbol` | `char` | Symbol gracza mającego teraz ruch (`'X'` lub `'O'`) |
| `winnerName` | `String` | Nazwa zwycięzcy lub `null` |
| `winnerSymbol` | `char` | Symbol zwycięzcy lub `' '` |
| `playerCount` | `int` | Liczba podłączonych graczy (1 lub 2) |

**Metoda `renderBoard()`:** renderuje planszę do czytelnego tekstu z ramką i indeksami (wiersze 0–2, kolumny 0–2).

---

### `MoveResult` (Serializable)

Wynik wywołania `makeMove()`, przesyłany przez RMI jako kopia.

| Pole | Typ | Opis |
|------|-----|------|
| `valid` | `boolean` | Czy ruch został zaakceptowany |
| `message` | `String` | Komunikat dla gracza (błąd lub info o stanie) |
| `gameState` | `GameState` | Stan gry po ruchu (lub bieżący przy odrzuceniu) |

---

### Server `Main.java`

1. Pyta o hostname/IP serwera.
2. Ustawia `System.setProperty("java.rmi.server.hostname", host)` – wymagane gdy serwer ma wiele interfejsów sieciowych.
3. Tworzy RMI Registry: `LocateRegistry.createRegistry(1099)`.
4. Tworzy instancję `TicTacToeServiceImpl`.
5. Rejestruje ją: `Naming.rebind("//" + host + "/TicTacToe", gameService)`.
6. Czeka na Enter, po czym kończy pracę.

---

### Client `Main.java`

**Metody:**

| Metoda | Opis |
|--------|------|
| `main()` | Nawiązuje połączenie RMI, wywołuje `joinGame()`, uruchamia pętlę gry. |
| `waitForGameStart()` | Polluje `getGameState()` co sekundę aż status != `WAITING_FOR_PLAYER`. |
| `playGame()` | Główna pętla: pobiera stan, wyświetla planszę, obsługuje ruchy i zakończenie. |
| `readMove()` | Wczytuje wiersz i kolumnę z konsoli; walidacja formatu po stronie klienta. |
| `askPlayAgain()` | Po zakończeniu gry pyta o reset; wywołuje `game.resetGame()`. |

---

## 5. Przepływ działania aplikacji

```
SERWER                                    KLIENT 1 (X)              KLIENT 2 (O)
   │                                           │                         │
   │  createRegistry(1099)                     │                         │
   │  rebind("TicTacToe")                      │                         │
   │  [czeka na graczy]                        │                         │
   │                                           │  Naming.lookup(...)     │
   │◄──────────────────────────────────────────│                         │
   │  joinGame("Gracz1") → "id1:X"             │                         │
   │──────────────────────────────────────────►│                         │
   │  [1 gracz, czeka na O]                    │  polluje getGameState() │
   │                                           │  (WAITING_FOR_PLAYER)   │
   │                                           │                         │  Naming.lookup(...)
   │◄────────────────────────────────────────────────────────────────────│
   │  joinGame("Gracz2") → "id2:O"             │                         │
   │  [gra startuje, status=IN_PROGRESS]       │                         │
   │────────────────────────────────────────────────────────────────────►│
   │                                           │  getGameState()         │
   │  status=IN_PROGRESS ─────────────────────►│                         │
   │                                           │  [wyświetla planszę]    │
   │                                           │  makeMove(id1, 1, 1)    │
   │◄──────────────────────────────────────────│  (środek planszy)       │
   │  [walidacja OK, X na [1][1]]              │                         │
   │  MoveResult(valid=true) ─────────────────►│                         │
   │                                           │                         │  isMyTurn(id2)=true
   │                                           │                         │  makeMove(id2, 0, 0)
   │◄────────────────────────────────────────────────────────────────────│
   │  [O na [0][0], gra trwa]                  │                         │
   │  MoveResult(valid=true) ────────────────────────────────────────────►│
   │  ...kolejne ruchy...                      │                         │
   │                                           │                         │
   │  checkWin → true                          │                         │
   │  status = WIN                             │                         │
   │  MoveResult("X wygrywa!") ───────────────►│  ">>> WYGRYWASZ! <<<"   │
   │  getGameState() → WIN ───────────────────────────────────────────── ►│  ">>> PRZEGRYWASZ <<<"
```

---

## 6. Instrukcja uruchomienia

### Wymagania
- **Java JDK 11** (np. Microsoft Build of OpenJDK 11)
- IntelliJ IDEA

### Krok 1 – Kompilacja i uruchomienie serwera

1. Otwórz moduł `server` w IntelliJ IDEA.
2. Uruchom klasę `Main` (Run → Run 'Main').
3. Podaj hostname/IP serwera:
   - **lokalna sieć (2 komputery):** wpisz IP komputera serwera, np. `192.168.1.10`
   - **ten sam komputer:** wciśnij Enter (domyślnie `localhost`)

```
=== KKSerwer – Gra Kółko i Krzyżyk (RMI) ===
Podaj hostname/IP serwera [localhost]: 192.168.1.10
[Serwer] RMI Registry uruchomione na porcie 1099.
[Serwer] Serwer gry zarejestrowany pod: //192.168.1.10/TicTacToe
[Serwer] Oczekiwanie na graczy...
```

### Krok 2 – Uruchomienie klientów

Na **każdym komputerze gracza** (lub w dwóch oknach terminala na tym samym komputerze):

1. Otwórz moduł `client` w IntelliJ IDEA.
2. Uruchom klasę `Main`.
3. Podaj hostname/IP **serwera** (ten sam co w kroku 1).
4. Podaj nazwę gracza.

```
=== KKlient – Gra Kółko i Krzyżyk (RMI) ===
Podaj hostname/IP serwera [localhost]: 192.168.1.10
Podaj swoją nazwę: Marek
[Klient] Połączono z serwerem.
[Klient] Dołączono do gry jako 'X'.
```

### Prezentacja na 2 komputerach

```
Komputer A (serwer + klient 1):          Komputer B (klient 2):
  java Main serwera → IP: 10.0.0.1         java Main klienta → IP serwera: 10.0.0.1
  java Main klienta → IP serwera: localhost
```

> **Ważne:** Java RMI wymaga żeby `java.rmi.server.hostname` na serwerze był adresem **osiągalnym przez klientów**. Przy pracy w sieci lokalnej podaj IP komputera serwera (nie `localhost`).

---

## 7. Jak wygląda rozgrywka

### Plansza
```
    0   1   2  
  +---+---+---+
0 | X | O |   |
  +---+---+---+
1 |   | X |   |
  +---+---+---+
2 | O |   | X |
  +---+---+---+
```
Pozycje: **wiersz** 0–2 (góra–dół), **kolumna** 0–2 (lewa–prawa).

### Przebieg gry

1. Gracz X wpisuje wiersz i kolumnę.
2. Serwer waliduje ruch i odsyła `MoveResult`.
3. Gracz O widzi aktualizację planszy po kolejnym `getGameState()`.
4. Gra kończy się gdy ktoś ułoży 3 w rzędzie/kolumnie/przekątnej lub gdy plansza jest pełna (remis).

### Błędy walidacji (po stronie serwera)

| Sytuacja | Komunikat |
|----------|-----------|
| Ruch nie w swojej kolejności | `"Nie twoja kolej!"` |
| Pole zajęte | `"Pole (r,c) jest już zajęte."` |
| Pozycja poza planszą | `"Pozycja musi być w zakresie 0–2."` |
| Gra już zakończona | `"Gra już się zakończyła."` |

### Reset gry

Po zakończeniu partii obaj gracze zostaną zapytani o nową grę. Klient który odpowie `T` wywoła `resetGame()` na serwerze – plansza jest czyszczona, gracze pozostają zarejestrowani, X zaczyna ponownie.
