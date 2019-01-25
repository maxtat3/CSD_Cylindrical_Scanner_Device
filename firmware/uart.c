#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <util/atomic.h>
#include <stdbool.h>

#include "bit_macros.h"
#include "uart.h"


// Request (RX, receive) buffer
static volatile uint8_t rxBuff[RX_BUFF_SIZE];

// Response (TX, translate) buffer
static volatile uint8_t txBuff[TX_BUFF_SIZE];

// Counter received Bytes
static volatile uint8_t rxCnt = 0;



void initUSART(){
	//UBRR=95 @ 9600 бод при 14,7456 MHz (U2X = 0)
	//UBRR=51 @ 9600 бод при 8 MHz (U2X = 0)
	// примерно 60 выб/с для 1 канала ???!!!
	UBRRH = 0;
	UBRRL = 51; 
	
	UCSRB=(1<<RXCIE)|(1<<RXEN)|(1<<TXEN); //разр. прерыв при приеме, разр приема, разр передачи.
	UCSRC=(1<<URSEL)|(1<<UCSZ1)|(1<<UCSZ0);  //размер слова 8 разрядов
}


void sendPckToUART(uint8_t *pck){
	for (uint8_t i = 0; i < TX_BUFF_SIZE; i++){
		txBuff[i] = *(pck+i);
	}
	sendBuffToUART();
}

static void sendBuffToUART(void){	// private
	sbi(UCSRB, UDRIE); 
}

/*
 * @brief Вектор прерывания для передачи пакета к клиенту
 */
ISR(USART_UDRE_vect){
	for(uint8_t i = 0; i < TX_BUFF_SIZE; i++){		// todo: changed uint8_t -> uint16_t
		UDR = txBuff[i];
		while(!(UCSRA & (1<<UDRE)));	// TODO  возможно поменять месаим while and UDR !!!
	}
	cbi(UCSRB, UDRIE);		// disallow interrupt send of TX buffer
}


void getPckOfUART(uint8_t *rxBuffPr){
	if (rxCnt < RX_BUFF_SIZE){
		*(rxBuffPr) = 0;
		*(rxBuffPr+1) = 0;
		*(rxBuffPr+2) = 0;
		*(rxBuffPr+3) = 0;
		return;
	} 

	ATOMIC_BLOCK(ATOMIC_FORCEON){
		*(rxBuffPr) = rxBuff[0];
		*(rxBuffPr+1) = rxBuff[1];
		*(rxBuffPr+2) = rxBuff[2];
		*(rxBuffPr+3) = rxBuff[3];
	}

	rxCnt = 0;
}


/*
 * @brief Вектор прерывания для приема пактеа от сервера 
 */
ISR(USART_RXC_vect){ 
	rxBuff[rxCnt] = UDR;
	rxCnt++;
} 
