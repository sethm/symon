;;
;; Output the string 'Hello, World!'
;;


.alias cr  $0d
.alias lf  $0a

.alias out $c000

.org $0300

start:  ldx #$00
loop:   lda string,x
        beq start      ; If A is 0, loop back and start again
        sta out        ; Otherwise, store into output
        inx            ; Increment X
        jmp loop       ; Repeat.

string: .byte "Hello, 6502 world! ", 0
