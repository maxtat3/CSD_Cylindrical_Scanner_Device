#ifndef UART_H
#define UART_H

#include <avr/io.h>

// Request from client.
// Request buffer size in Bytes. 
#define 	RX_BUFF_SIZE		4

// Response to client.
// Response buffer size in Bytes. 
#define 	TX_BUFF_SIZE		4


// Initialisation of IART module
void initUSART(void);

// Send package (response) to server
// todo - or rename rspPckToClient
void sendPckToUART(uint8_t *pck);

// Allow interrupt for send of TX buffer
// private visibility
static void sendBuffToUART(void);

/*
* @brief Receive package from client.
* @param rxBuffPr - pointer to primary array buffer.
*/
// todo - or rename rqPckFromClient
void getPckOfUART(uint8_t *rxBuffPr);


#endif // UART_H
