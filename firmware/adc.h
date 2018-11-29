#ifndef ADC_H
#define ADC_H

#include <avr/io.h>


void initADC(void);
void start_cont_conv(void); // start continious converion adc mode.
uint16_t get_adc_res(void);


#endif // ADC_H