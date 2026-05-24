package cc.arccore.api.command

/**
 * KSP 소스 생성용 어노테이션.
 * 클래스에 붙여서 커맨드 메타데이터를 KSP 프로세서에 전달한다.
 * 이름 충돌 방지를 위해 ARCCommand 인터페이스와 별개 파일로 관리한다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class CommandSpec(
    val name: String,
    val aliases: Array<String> = [],
    val permission: String = "",
    val description: String = "",
    val usage: String = ""
)
