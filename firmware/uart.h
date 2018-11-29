#ifndef UART_H
#define UART_H

#include <avr/io.h>



#define 	TC0_TCNT_VAL	153



void initUSART(void);
void sendCmdAndDataToUSART(uint8_t cmd, uint8_t data);
void sendCmdToUSART(uint8_t cmd);
void sendDataToUSART(uint8_t data);
void writeCharToUSART(uint8_t sym);
uint8_t getCharOfUSART(void);

void runTC0(void);
void stopTC0(void);


#endif // UART_H
