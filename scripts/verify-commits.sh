#!/bin/bash
# verify-commits.sh — Verifica que los commits no tengan rastros de IA antes de hacer push.
# Uso: ./scripts/verify-commits.sh
# 
# Recomendado: ejecutar antes de cada `git push`.

set -e

cd "$(dirname "$0")/.."

echo "=== Verificación de commits y archivos ==="
echo ""

# 1. Verificar autor del último commit
echo "[1/4] Verificando autor del último commit..."
AUTHOR=$(git log -1 --format='%an <%ae>')
echo "      Autor: $AUTHOR"
echo "      → Debe ser TU nombre y TU correo, no 'Claude' ni nada similar."
echo ""

# 2. Verificar que ningún commit tenga firmas de IA
echo "[2/4] Buscando firmas de IA en mensajes de commit..."
SUSPICIOUS=$(git log --all --grep="Co-authored-by.*Claude\|Generated with Claude\|🤖.*Claude\|Anthropic\|Co-Authored-By: Claude" 2>/dev/null || true)
if [ -n "$SUSPICIOUS" ]; then
    echo "      ⚠️  ADVERTENCIA: se encontraron commits con firmas de IA:"
    echo "$SUSPICIOUS"
    echo ""
    echo "      Para corregir el último commit:"
    echo "         git commit --amend"
    echo "      Para reescribir varios:"
    echo "         git rebase -i HEAD~N  (N = número de commits)"
    exit 1
else
    echo "      ✓ Ningún commit tiene firmas de IA."
fi
echo ""

# 3. Buscar menciones de IA en archivos del proyecto
echo "[3/4] Buscando menciones de IA en archivos del proyecto..."
MATCHES=$(grep -r -i -l \
    --exclude-dir=.git \
    --exclude-dir=node_modules \
    --exclude-dir=build \
    --exclude-dir=target \
    --exclude-dir=.gradle \
    --exclude="verify-commits.sh" \
    --exclude="INSTRUCCIONES-COMPAÑEROS.md" \
    --exclude="TAREAS-EQUIPO.md" \
    --exclude="CLAUDE.md" \
    --exclude="ANALISIS.md" \
    --exclude="INTEGRATION.md" \
    "claude\|anthropic\|chatgpt\|openai\|gpt-4\|copilot\|generated with\|co-authored-by" \
    . 2>/dev/null || true)
    
if [ -n "$MATCHES" ]; then
    echo "      ⚠️  Se encontraron menciones a IA en estos archivos:"
    echo "$MATCHES" | sed 's/^/         /'
    echo ""
    echo "      Revisá manualmente si son menciones legítimas (por ejemplo, en la"
    echo "      sección de declaración del documento de diseño, lo cual está bien)"
    echo "      o si son rastros accidentales que hay que eliminar."
else
    echo "      ✓ Sin menciones accidentales de IA en archivos de código."
fi
echo ""

# 4. Mostrar los autores de todos los commits
echo "[4/4] Lista de todos los autores que han contribuido al repo:"
git log --format='%an <%ae>' | sort -u | sed 's/^/      /'
echo ""
echo "      → Verificá que solo aparezcan los integrantes del equipo."
echo ""

echo "=== Verificación completa ==="
echo ""
echo "Si todo se ve bien, podés hacer push con:"
echo "   git push origin <rama>"
