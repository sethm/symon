;;
;; Output the string 'Hello, World!'
;;


.alias iobase   $8800
.alias iostatus [iobase + 1]
.alias iocmd    [iobase + 2]
.alias ioctrl   [iobase + 3]

.org $0300

start:  cli
        lda #$0b
        sta iocmd      ; Set command status
        lda #$1a
        sta ioctrl     ; 0 stop bits, 8 bit word, 2400 baud

init:   ldx #$00       ; Initialize index

loop:   lda iostatus
        and #$10       ; Is the tx register empty?
        beq loop       ; No, wait for it to empty
        lda string,x   ; Otherwise, load the string pointer
        beq init       ; If the char is 0, re-init
        sta iobase     ; Otherwise, transmit

        inx            ; Increment string pointer.
        jmp loop       ; Repeat write.

string: .byte "Hello, 6502 world! ", 0
