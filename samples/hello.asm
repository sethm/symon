;;
;; Output the string 'Hello, World!'
;;


.alias iobase   $c000
.alias iostatus [iobase + 1]
.alias iocmd    [iobase + 2]
.alias ioctrl   [iobase + 3]

.org $0300

start:  cli
        lda #$09
        sta iocmd      ; Set command status
        lda #$16
        sta ioctrl     ; 0 stop bits, 8 bit word, 300 baud
        ldx #$00       ; Initialize index

loop:   lda string,x
        beq start      ; If A is 0, loop back and start again

write:  lda iostatus
        and #$10       ; Load ACIA status. Is output buffer empty?
        beq write      ; If not, loop back and try again,
        lda string,x
        sta iobase     ; Otherwise, write to output.

        inx
        jmp loop       ; Repeat.

string: .byte "Hello, 6502 world! ", 0
