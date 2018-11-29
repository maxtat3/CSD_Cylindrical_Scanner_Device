#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <util/atomic.h>
#include <stdbool.h>

#include "bit_macros.h"
#include "uart.h"


volatile uint8_t usartRxBuf = 0;	//однобайтный буфер


// Временные переменные для команды и данных
uint8_t cmdTmp = 0;
uint8_t dataTmp = 0;




void initUSART(){
	//UBRR=95 @ 9600 бод при 14,7456 MHz (U2X = 0)
	//UBRR=51 @ 9600 бод при 8 MHz (U2X = 0)
	// примерно 60 выб/с для 1 канала ???!!!
	UBRRH = 0;
	UBRRL = 51; 
	
	UCSRB=(1<<RXCIE)|(1<<RXEN)|(1<<TXEN); //разр. прерыв при приеме, разр приема, разр передачи.
	UCSRC=(1<<URSEL)|(1<<UCSZ1)|(1<<UCSZ0);  //размер слова 8 разрядов
}


// Отправка в COM порт команды затем данных
void sendCmdAndDataToUSART(uint8_t cmd, uint8_t data){
	cmdTmp = cmd;
	dataTmp = data;
	runTC0();
}

// ОТправка в COM порт только команды
// Данные = 0
void sendCmdToUSART(uint8_t cmd){	// todo - not used in main.c
	cmdTmp = cmd;
	dataTmp = 0;
	runTC0();
}

// Отправка в COM порт только данных
// Команда = 0
void sendDataToUSART(uint8_t data){
	cmdTmp = 0;
	dataTmp = data;
	runTC0();
}

// отправка символа по usart`у
void writeCharToUSART(uint8_t sym){
	while(!(UCSRA & (1<<UDRE)));
	UDR = sym;  
}

// чтение буфера usart
uint8_t getCharOfUSART(void){
	uint8_t tmp;
	ATOMIC_BLOCK(ATOMIC_FORCEON){
		tmp = usartRxBuf;
		usartRxBuf = 0;
	}
	return tmp;  
}


// Прием символа по usart`у в буфер
ISR(USART_RXC_vect){ 
   usartRxBuf = UDR;  
} 





// Запуска ТС0 в прерывании которого 
// выполняеться отправка Байта команды и данных по UART.
void runTC0(){
	sbi(TIMSK, TOIE0);

	TCNT0 = TC0_TCNT_VAL;

	//делитель 1024
	sbi(TCCR0, 	CS00);
	cbi(TCCR0,	CS01);
	sbi(TCCR0,	CS02);
}

// Остановка ТС0. После отправки Байта команды и 
// данных по UART таймер должен вызвать этот метод , тем самым
// сам остановвиться.
void stopTC0(){
	cbi(TCCR0,	CS00);
	cbi(TCCR0,	CS01);
	cbi(TCCR0,	CS02);
}



volatile bool isCmd = true;

ISR(TIMER0_OVF_vect){
	if (isCmd){
		writeCharToUSART(cmdTmp);
		cmdTmp = 0;
		isCmd = false;
	} else {
		writeCharToUSART(dataTmp);
		dataTmp = 0;
		isCmd = true;
		stopTC0();
		return;
	}

	TCNT0 = TC0_TCNT_VAL;
}