// Общий курио-слот: валидатор kubejs:any_curio
// Принимает предмет, если он ICurio или помечен любым предметным тегом namespace "curios".
// ВАЖНО: внутри тела валидатора НЕТ локальных переменных (const/let внутри функции,
// вызываемой через Rhino InterfaceAdapter, дают "redeclaration of var ..." при повторных вызовах).

const SharedCuriosApi = Java.loadClass('top.theillusivec4.curios.api.CuriosApi')
const SharedResLoc = Java.loadClass('net.minecraft.resources.ResourceLocation')

function sharedNamespaceOf(holderObj) {
    if (holderObj == null) {
        return ''
    }
    try {
        if (typeof holderObj.namespace === 'function') {
            return '' + holderObj.namespace()
        }
    } catch (firstTryErr) {}
    try {
        return '' + holderObj.key().namespace()
    } catch (secondTryErr) {}
    try {
        return '' + holderObj.location().namespace()
    } catch (thirdTryErr) {}
    return ''
}

const anyCurioPredicate = slotResultObj => {
    try {
        return slotResultObj != null &&
            slotResultObj.stack() != null &&
            !slotResultObj.stack().isEmpty() &&
            (SharedCuriosApi.getCurio(slotResultObj.stack()).isPresent() ||
             slotResultObj.stack().getItem().builtInRegistryHolder().tags()
                 .anyMatch(h => sharedNamespaceOf(h) === 'curios'))
    } catch (predicateErr) {
        console.error('[curios_shared_slot] predicate error: ' + predicateErr)
        return false
    }
}

const registerSharedValidator = () => {
    try {
        SharedCuriosApi.registerCurioPredicate(SharedResLoc.fromNamespaceAndPath('kubejs', 'any_curio'), anyCurioPredicate)
        console.info('[curios_shared_slot] validator kubejs:any_curio registered')
    } catch (registerErr) {
        console.error('[curios_shared_slot] register failed: ' + registerErr)
    }
}

registerSharedValidator()
StartupEvents.init(ignoredEvent => {
    registerSharedValidator()
})
