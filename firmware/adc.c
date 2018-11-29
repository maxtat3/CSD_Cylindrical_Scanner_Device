#include <avr/io.h>
#include <avr/interrupt.h>

#include "adc.h"
#include "bit_macros.h"

volatile uint8_t lowByte; // младший байт ацп преобразования
volatile uint16_t adcResult; // результат ацп 


// настройка АЦП
void initADC(void){
	ADCSRA |= (1<<ADPS2)|(1<<ADPS1)|(1<<ADPS0); // предделитель на 128
	ADCSRA |= (1<<ADIE);                        // разрешаем прерывание от ацп
	ADCSRA |= (1<<ADEN);                        // разрешаем работу АЦП

	ADMUX |= (1<<REFS0)|(1<<REFS1);             // работа от внутр. ИОН 2,56 В
	ADMUX|=(0<<MUX3)|(0<<MUX2)|(0<<MUX1)|(0<<MUX0);
}

void start_cont_conv(void){
	ADCSRA |= (1<<ADSC); 
}

uint16_t get_adc_res(void){
	return adcResult;
}


// Обработка прерывания от ацп
ISR(ADC_vect){
	// считываем младший и старший байты результата АЦ-преобразования и образуем из них 10-разрядный результат
	lowByte = ADCL;
	adcResult = (ADCH<<8)|lowByte;

	// запускаем новое АЦ-преобразование
	ADCSRA |= (1<<ADSC);
}