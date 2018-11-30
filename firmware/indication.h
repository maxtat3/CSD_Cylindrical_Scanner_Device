#ifndef INDICATION_H
#define INDICATION_H

#include <avr/io.h>
#include <stdbool.h>
#include "bit_macros.h"


#define		LED_MSR_PORT		PORTB
#define		LED_MSR_DDR		DDRB
#define 	LED_MSR 		1

// готовый набор интервалов для ф-ии ind_led_msr_state

#define		LED_BLINK_FAST		10
#define		LED_BLINK_MEDIUM	50
#define		LED_BLINK_SLOW		100


// Инициализация портов 
// Настриваем порт на ВЫХОД для подключения светодиода 
void init_ind_led_msr(void);

// Индикация выполнения процесса измерений в виде светодиода.
// @param state - состояние светодиода: вкл/откл
// state == false - измерение не выполняется или остановленно
// state == true - выполняется процесс измерений
// 
// @param blink - включение мерцания
// 
// @param interval - интервал мерцания светодиода с определенной частотой. 
// Разная частота может быть применена для индикации нескольих событий при помощи одного светодиода.
// Частота задается количеством едииц в параметре @interval при которых светодиод включен или выключен
// и определяется внешней частотй вызова этой ф-ии, например счетчиком. 
// 
// Эта ф-ия может быть полезна при индикации события внутри прерываний.
void ind_led_msr_state(bool state, bool blink, uint8_t interval);

#endif // INDICATION_H