// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed.
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.
(LOOP)
    @KBD
    D = M
    @WHITE
    D; JEQ
(BLACK)
    @65534
    D=A
    @R1
    M=D
    @WRITE
    0; JMP
(WHITE)
    @0
    D=A
    @R1
    M=D
(WRITE)
    @R0
    M = 0
(LOOP1)
    @R0
    D = M
    @SCREEN
    D = A + D
    @R2
    M = D
    @R1
    D = M
    @R2
    A = M
    M = D
    @R0
    M = M + 1
    D = M
    @5
    D = D - A
    @LOOP
    D; JEQ
    @LOOP1
    0; JEQ
