;;
;; Read input from the keyboard, and echo to console.
;;


IOBASE   = $8800
IOSTATUS = IOBASE + 1
IOCMD    = IOBASE + 2
IOCTRL   = IOBASE + 3

.segment "CODE"
.org $C000

START:  CLI
        LDA #$09
        STA IOCMD      ; Set command status
        LDA #$1A
        STA IOCTRL     ; 0 stop bits, 8 bit word, 2400 baud

;;
;; Infinite idle loop, waiting for interrupt.
;;
IDLE:   LDA #$FF
        TAX
IDLE1:  DEX
        BNE IDLE1
        JMP IDLE

;;
;; IRQ handler
;;
IRQ:    LDA IOBASE      ; Get the character in the ACIA.
        PHA             ; Save accumulator
ECHO1:  LDA IOSTATUS    ; Read the ACIA status
        AND #$10        ; Is the tx register empty?
        BEQ ECHO1       ; No, wait for it to empty
        PLA             ; Otherwise, load saved accumulator,
        STA IOBASE      ; write to output,
        RTI             ; and return

; system vectors

.segment "VECTORS"
.org    $FFFA

.word   IRQ         ; NMI vector
.word   START       ; RESET vector
.word   IRQ         ; IRQ vector
