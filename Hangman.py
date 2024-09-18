import random

# Function to load words from file and return a random word
def get_random_word(file_path):
    with open(file_path, 'r') as file:
        # Read all lines (words) from the file and strip any whitespace or newline characters
        words = [line.strip() for line in file.readlines()]
    
    # Use random.choice to select a random word from the list
    random_word = random.choice(words)
    return random_word

# Function to get User Input
def get_letter():
    while True:
        user_input = input("\nPlease enter a letter in lowercase: ")

        # Check if the input length is exactly one
        if len(user_input) == 1:
            return user_input
        else:
            print("Invalid input. Please enter one letter at a time.")

# Path to your text file
file_path = 'C:\\Users\\Azaze\\Downloads\\updated_words.txt'
random_word = get_random_word(file_path)

mistake = 0
max_mistakes = 7
guess = ['_'] * len(random_word)
guessed_letters = []

while mistake < max_mistakes:
    print("Word: " + ' '.join(guess))  # Display the current state of the word
    print("Mistakes: " + str(mistake) + "/" + str(max_mistakes))
    print(f"Guessed letters: {', '.join(guessed_letters)}")
    letter = get_letter()  # Get user input
    
    # Check if the letter has already been guessed
    if letter in guessed_letters:
        print(f"You already guessed '{letter}'. Try a different letter.")
        continue

    # Add the letter to the list of guessed letters
    guessed_letters.append(letter)

    if letter in random_word:  # Check if the guessed letter is in the word
        for i in range(len(random_word)):
            if random_word[i] == letter:
                guess[i] = letter  # Reveal the letter in the correct position
    else:
        
        mistake += 1  # Increment mistake counter

    if mistake == max_mistakes:
        print("You lost! The correct word was: ", random_word)
        break

    if ''.join(guess) == random_word:  # Check if the user has guessed the whole word
        print("Congratulations! You guessed the word:", random_word)
        break