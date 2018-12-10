/*
* Опто-прерыватель с открытым каналом.
* Применен как датчик остановки шагового двигателя (ШД).
*
* Оптопара: светодиод и фото-транзистор
* Нормально-откр. сосотяние
* Тип выхода фото-транзистора: NPN 
* Окно открыто (норм. сост.) Output -> High level
* Окно заккрыто (уст. преграда) Output -> Low level
*/

#ifndef OPTO_INTERRUPTER_H
#define OPTO_INTERRUPTER_H

#include <avr/io.h>
#include <stdbool.h>
#include "bit_macros.h"


/* Определение регистров порта к которму подключен опто-прерыватель */
#define		OPTO_INTRPT_PORT	PORTB
#define		OPTO_INTRPT_DDR		DDRB
#define		OPTO_INTRPT_PIN		PINB

/* Номер порта к которому подключен опто-прерыватель*/
#define		OPTO_INTRPT		2


/* Настриваем на ВХОД порт для подключения опто-прерывателя */
void init_opto_interrupter(void);

/*
* Проверка закрыто ли окно опто-прерывателя.
*
* @return 
* 	true - окно закрыто внешней преградой 
*	false - окно открыто - свет нормально проходит на фото-детектор (нормальное состояние)
*/
bool opto_intrpt_is_closed(void);


#endif // OPTO_INTERRUPTER_H
