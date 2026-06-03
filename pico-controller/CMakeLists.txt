cmake_minimum_required(VERSION 3.13)

# Pico SDK setup
# Hardware objetivo: Raspberry Pi Pico clasico (RP2040 sin CYW43).
# El LED interno del Pico clasico esta cableado al GP25 y se maneja como GPIO normal.
set(PICO_BOARD pico CACHE STRING "Pico board")
include(pico_sdk_import.cmake)

project(spaceinvaders_controller C CXX ASM)
set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

pico_sdk_init()

add_executable(controller
    src/main.c
    src/button.c
    src/uart_sender.c
)

# Firmware de diagnostico minimo: solo parpadea el LED, sin USB ni botones.
# Para activarlo: cmake --build . --target blink_test  y flashear blink_test.uf2
add_executable(blink_test
    src/main_blink_test.c
)
target_link_libraries(blink_test pico_stdlib hardware_gpio)
pico_enable_stdio_usb(blink_test 0)
pico_enable_stdio_uart(blink_test 0)
pico_add_extra_outputs(blink_test)

target_include_directories(controller PRIVATE include)

target_link_libraries(controller
    pico_stdlib
    hardware_uart
    hardware_gpio
)

# Si se quiere debug por USB-CDC (printf via USB) habilitarlo, pero NO mezclar
# con la salida del cliente C porque ambos abririan /dev/ttyACM0.
# Para spaCEinvaders dejamos USB-CDC apagado y UART0 hacia el cliente C real.
pico_enable_stdio_usb(controller 0)
pico_enable_stdio_uart(controller 0)

pico_add_extra_outputs(controller)
