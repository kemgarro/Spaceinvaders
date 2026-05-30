cmake_minimum_required(VERSION 3.13)

# Pico SDK setup
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
