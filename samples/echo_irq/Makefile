CA=ca65
LD=ld65

all: echo_irq

echo_irq: echo_irq.o
	$(LD) -C symon.config -vm -m echo_irq.map -o echo_irq.rom echo_irq.o

echo_irq.o:
	$(CA) --listing echo_irq.lst -o echo_irq.o echo_irq.asm

clean:
	rm -f *.o *.rom *.map *.lst
